package com.stableflow.blockchain.service;

import com.stableflow.blockchain.client.SolanaClient;
import com.stableflow.blockchain.entity.PaymentScanCursor;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.config.SolanaScanProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentScanServiceImpl implements PaymentScanService {

    private static final Logger log = LoggerFactory.getLogger(PaymentScanService.class);
    private static final String DEFAULT_CURRENCY_USDC = "USDC";
    private static final String DEFAULT_CURRENCY_UNKNOWN = "UNKNOWN";

    private final SolanaClient solanaClient;
    private final MerchantPaymentConfigService merchantPaymentConfigService;
    private final PaymentScanCursorService paymentScanCursorService;
    private final PaymentTransactionService paymentTransactionService;
    private final SolanaProperties solanaProperties;
    private final SolanaScanProperties solanaScanProperties;
    private final ObjectMapper objectMapper;

    @Override
    public int scanAllActiveAddresses() {
        int totalInserted = 0;
        // 只扫描启用中的商家收款地址，先把 MVP 主链路收敛在有效配置上。
        for (MerchantPaymentConfig paymentConfig : merchantPaymentConfigService.listActiveConfigs()) {
            try {
                totalInserted += scanRecipientAddress(paymentConfig);
            } catch (RuntimeException ex) {
                log.error(
                    "Skipping failed recipientAddress={} for merchantId={} due to scan error",
                    paymentConfig.getWalletAddress(),
                    paymentConfig.getMerchantId(),
                    ex
                );
            }
        }
        return totalInserted;
    }

    @Override
    public int scanRecipientAddress(MerchantPaymentConfig paymentConfig) {
        String recipientAddress = paymentConfig.getWalletAddress();
        PaymentScanCursor cursor = paymentScanCursorService.getOrCreate(recipientAddress);

        // 只拉取上次扫描游标之后出现的新签名，避免每次都全量回扫历史交易。
        List<SolanaTransactionSignatureVo> candidateSignatures = fetchNewSignatures(
            recipientAddress,
            cursor.getLastSeenSignature(),
            resolveBatchSize()
        );

        if (candidateSignatures.isEmpty()) {
            paymentScanCursorService.updateCursor(recipientAddress, cursor.getLastSeenSignature());
            return 0;
        }

        String newestProcessedSignature = candidateSignatures.getFirst().getSignature();
        List<SolanaTransactionSignatureVo> signaturesToProcess = new ArrayList<>(candidateSignatures);
        // 按从旧到新的顺序处理，后续验证和核销看到的交易时序会更稳定。
        Collections.reverse(signaturesToProcess);

        int insertedCount = 0;
        for (SolanaTransactionSignatureVo signatureVo : signaturesToProcess) {
            SolanaTransactionDetailVo transactionDetail = solanaClient.getTransaction(signatureVo.getSignature());
            if (transactionDetail == null) {
                continue;
            }

            if (paymentTransactionService.saveIfAbsent(toPaymentTransaction(transactionDetail))) {
                insertedCount++;
            }
        }

        // 整个地址批次处理完成后再推进游标，避免处理中途失败导致交易漏扫。
        paymentScanCursorService.updateCursor(recipientAddress, newestProcessedSignature);
        log.info(
            "Scanned recipientAddress={}, newSignatures={}, inserted={}",
            recipientAddress,
            candidateSignatures.size(),
            insertedCount
        );
        return insertedCount;
    }

    private List<SolanaTransactionSignatureVo> fetchNewSignatures(String address, String lastSeenSignature, int batchSize) {
        List<SolanaTransactionSignatureVo> collected = new ArrayList<>();
        String beforeSignature = null;
        boolean reachedExistingCursor = false;

        while (true) {
            // 按签名分页向历史方向翻页，直到碰到上次记录的游标为止。
            List<SolanaTransactionSignatureVo> batch = solanaClient.getSignaturesForAddress(address, batchSize, beforeSignature);
            if (batch == null || batch.isEmpty()) {
                break;
            }

            for (SolanaTransactionSignatureVo signatureVo : batch) {
                if (lastSeenSignature != null && lastSeenSignature.equals(signatureVo.getSignature())) {
                    // 一旦命中旧游标，说明后面的签名都已经扫描过了，可以停止收集。
                    reachedExistingCursor = true;
                    break;
                }
                collected.add(signatureVo);
            }

            if (reachedExistingCursor || batch.size() < batchSize) {
                break;
            }
            beforeSignature = batch.getLast().getSignature();
        }

        return collected;
    }

    private PaymentTransaction toPaymentTransaction(SolanaTransactionDetailVo transactionDetail) {
        // 把链上解析结果转换成数据库持久化模型，供后续验证和核销阶段继续使用。
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setTxHash(transactionDetail.getSignature());
        paymentTransaction.setReferenceKey(transactionDetail.getPrimaryReferenceKey());
        paymentTransaction.setPayerAddress(transactionDetail.getPayerAddress());
        paymentTransaction.setRecipientAddress(transactionDetail.getRecipientAddress());
        paymentTransaction.setAmount(transactionDetail.getAmount());
        paymentTransaction.setCurrency(resolveCurrency(transactionDetail.getMintAddress()));
        paymentTransaction.setMintAddress(transactionDetail.getMintAddress());
        paymentTransaction.setBlockTime(transactionDetail.getBlockTime());
        paymentTransaction.setVerificationResult(PaymentVerificationResultEnum.PENDING);
        paymentTransaction.setPaymentStatus(PaymentTransactionStatusEnum.DETECTED);
        paymentTransaction.setRawPayload(parseRawPayload(transactionDetail.getRawPayload()));
        return paymentTransaction;
    }

    private String resolveCurrency(String mintAddress) {
        if (mintAddress == null || mintAddress.isBlank()) {
            return DEFAULT_CURRENCY_UNKNOWN;
        }
        // MVP 阶段只把配置里的 USDC mint 识别成 USDC，其它资产先统一记为 UNKNOWN。
        return mintAddress.equals(solanaProperties.usdcMintAddress()) ? DEFAULT_CURRENCY_USDC : DEFAULT_CURRENCY_UNKNOWN;
    }

    private int resolveBatchSize() {
        return solanaScanProperties.batchSize() == null || solanaScanProperties.batchSize() <= 0
            ? 50
            : solanaScanProperties.batchSize();
    }

    private JsonNode parseRawPayload(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                ErrorCode.SYSTEM_ERROR,
                "Failed to parse Solana raw payload: " + ex.getOriginalMessage()
            );
        }
    }
}
