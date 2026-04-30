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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sol4k.PublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentScanServiceImpl implements PaymentScanService {

    private static final Logger log = LoggerFactory.getLogger(PaymentScanServiceImpl.class);
    private static final String DEFAULT_CURRENCY_USDC = "USDC";
    private static final String DEFAULT_CURRENCY_UNKNOWN = "UNKNOWN";
    private static final Comparator<SolanaTransactionSignatureVo> SIGNATURE_PROCESS_ORDER =
        Comparator.comparing(
                SolanaTransactionSignatureVo::getBlockTime,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing(SolanaTransactionSignatureVo::getSlot, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SolanaTransactionSignatureVo::getSignature, Comparator.nullsLast(Comparator.naturalOrder()));

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
                    "Skipping failed scan batch, merchantId={}, recipientAddress={}",
                    paymentConfig.getMerchantId(),
                    paymentConfig.getWalletAddress(),
                    ex
                );
            }
        }
        return totalInserted;
    }

    @Override
    public int scanRecipientAddress(MerchantPaymentConfig paymentConfig) {
        String recipientAddress = paymentConfig.getWalletAddress();
        List<ScanTargetBatch> scanTargetBatches = prepareScanTargetBatches(paymentConfig);
        Map<String, SolanaTransactionSignatureVo> uniqueCandidateSignatures = new LinkedHashMap<>();
        for (ScanTargetBatch scanTargetBatch : scanTargetBatches) {
            if (scanTargetBatch.candidateSignatures().isEmpty()) {
                paymentScanCursorService.updateCursor(scanTargetBatch.address(), scanTargetBatch.cursor().getLastSeenSignature());
                continue;
            }
            for (SolanaTransactionSignatureVo signatureVo : scanTargetBatch.candidateSignatures()) {
                uniqueCandidateSignatures.putIfAbsent(signatureVo.getSignature(), signatureVo);
            }
        }

        if (uniqueCandidateSignatures.isEmpty()) {
            return 0;
        }

        List<SolanaTransactionSignatureVo> signaturesToProcess = uniqueCandidateSignatures.values()
            .stream()
            .sorted(SIGNATURE_PROCESS_ORDER)
            .toList();

        int insertedCount = 0;
        for (SolanaTransactionSignatureVo signatureVo : signaturesToProcess) {
            try {
                SolanaTransactionDetailVo transactionDetail = solanaClient.getTransaction(signatureVo.getSignature());
                if (transactionDetail == null) {
                    continue;
                }

                PaymentTransaction paymentTransaction = toPaymentTransaction(transactionDetail);
                if (paymentTransaction == null) {
                    log.info(
                        "Skipping unsupported scanned transaction, recipientAddress={}, txHash={}, transferType={}, mintAddress={}",
                        recipientAddress,
                        transactionDetail.getSignature(),
                        transactionDetail.getTransferType(),
                        transactionDetail.getMintAddress()
                    );
                    continue;
                }

                if (paymentTransactionService.saveIfAbsent(paymentTransaction)) {
                    insertedCount++;
                }
            } catch (RuntimeException ex) {
                log.error(
                    "Failed to persist scanned transaction, recipientAddress={}, txHash={}",
                    recipientAddress,
                    signatureVo.getSignature(),
                    ex
                );
            }
        }

        // 每个扫描地址都在对应批次处理完成后再推进游标，避免处理中途失败导致交易漏扫。
        for (ScanTargetBatch scanTargetBatch : scanTargetBatches) {
            if (scanTargetBatch.candidateSignatures().isEmpty()) {
                continue;
            }
            paymentScanCursorService.updateCursor(scanTargetBatch.address(), scanTargetBatch.newestSignature());
        }
        log.info(
            "Payment scan batch finished, merchantId={}, recipientAddress={}, scanTargets={}, newSignatures={}, inserted={}",
            paymentConfig.getMerchantId(),
            recipientAddress,
            scanTargetBatches.stream().map(ScanTargetBatch::address).toList(),
            uniqueCandidateSignatures.size(),
            insertedCount
        );
        return insertedCount;
    }

    private List<ScanTargetBatch> prepareScanTargetBatches(MerchantPaymentConfig paymentConfig) {
        List<ScanTargetBatch> scanTargetBatches = new ArrayList<>();
        int batchSize = resolveBatchSize();

        for (String scanTargetAddress : resolveScanTargetAddresses(paymentConfig)) {
            PaymentScanCursor cursor = paymentScanCursorService.getOrCreate(scanTargetAddress);
            // 每个扫描目标都维护独立游标，这样主钱包和 ATA 都能增量回扫各自的新交易。
            List<SolanaTransactionSignatureVo> candidateSignatures = fetchNewSignatures(
                scanTargetAddress,
                cursor.getLastSeenSignature(),
                batchSize
            );
            scanTargetBatches.add(
                new ScanTargetBatch(
                    scanTargetAddress,
                    cursor,
                    candidateSignatures,
                    candidateSignatures.isEmpty() ? null : candidateSignatures.getFirst().getSignature()
                )
            );
        }
        return scanTargetBatches;
    }

    private List<String> resolveScanTargetAddresses(MerchantPaymentConfig paymentConfig) {
        LinkedHashSet<String> scanTargetAddresses = new LinkedHashSet<>();
        addIfPresent(scanTargetAddresses, paymentConfig.getWalletAddress());
        addIfPresent(
            scanTargetAddresses,
            deriveAssociatedTokenAddress(paymentConfig.getWalletAddress(), resolveScanMintAddress(paymentConfig))
        );
        return new ArrayList<>(scanTargetAddresses);
    }

    private String resolveScanMintAddress(MerchantPaymentConfig paymentConfig) {
        return hasText(paymentConfig.getMintAddress()) ? paymentConfig.getMintAddress() : solanaProperties.resolvedUsdcMintAddress();
    }

    private String deriveAssociatedTokenAddress(String walletAddress, String mintAddress) {
        if (!hasText(walletAddress) || !hasText(mintAddress)) {
            return null;
        }
        try {
            return PublicKey.findProgramDerivedAddress(new PublicKey(walletAddress), new PublicKey(mintAddress))
                .getPublicKey()
                .toBase58();
        } catch (RuntimeException ex) {
            log.warn(
                "Failed to derive associated token account, walletAddress={}, mintAddress={}, reason={}",
                walletAddress,
                mintAddress,
                ex.getMessage()
            );
            return null;
        }
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
        if (!hasText(transactionDetail.getRecipientAddress()) || transactionDetail.getAmount() == null) {
            return null;
        }

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
        return mintAddress.equals(solanaProperties.resolvedUsdcMintAddress()) ? DEFAULT_CURRENCY_USDC : DEFAULT_CURRENCY_UNKNOWN;
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void addIfPresent(LinkedHashSet<String> values, String candidate) {
        if (hasText(candidate)) {
            values.add(candidate);
        }
    }

    private record ScanTargetBatch(
        String address,
        PaymentScanCursor cursor,
        List<SolanaTransactionSignatureVo> candidateSignatures,
        String newestSignature
    ) {
    }
}
