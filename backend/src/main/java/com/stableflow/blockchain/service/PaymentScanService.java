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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentScanService {

    private static final Logger log = LoggerFactory.getLogger(PaymentScanService.class);
    private static final String PAYMENT_STATUS_DETECTED = "DETECTED";
    private static final String VERIFICATION_RESULT_PENDING = "PENDING";
    private static final String DEFAULT_CURRENCY_USDC = "USDC";
    private static final String DEFAULT_CURRENCY_UNKNOWN = "UNKNOWN";

    private final SolanaClient solanaClient;
    private final MerchantPaymentConfigService merchantPaymentConfigService;
    private final PaymentScanCursorService paymentScanCursorService;
    private final PaymentTransactionService paymentTransactionService;
    private final SolanaProperties solanaProperties;
    private final SolanaScanProperties solanaScanProperties;
    private final ObjectMapper objectMapper;

    public PaymentScanService(
        SolanaClient solanaClient,
        MerchantPaymentConfigService merchantPaymentConfigService,
        PaymentScanCursorService paymentScanCursorService,
        PaymentTransactionService paymentTransactionService,
        SolanaProperties solanaProperties,
        SolanaScanProperties solanaScanProperties,
        ObjectMapper objectMapper
    ) {
        this.solanaClient = solanaClient;
        this.merchantPaymentConfigService = merchantPaymentConfigService;
        this.paymentScanCursorService = paymentScanCursorService;
        this.paymentTransactionService = paymentTransactionService;
        this.solanaProperties = solanaProperties;
        this.solanaScanProperties = solanaScanProperties;
        this.objectMapper = objectMapper;
    }

    public int scanAllActiveAddresses() {
        int totalInserted = 0;
        for (MerchantPaymentConfig paymentConfig : merchantPaymentConfigService.listActiveConfigs()) {
            totalInserted += scanRecipientAddress(paymentConfig);
        }
        return totalInserted;
    }

    public int scanRecipientAddress(MerchantPaymentConfig paymentConfig) {
        String recipientAddress = paymentConfig.getWalletAddress();
        PaymentScanCursor cursor = paymentScanCursorService.getOrCreate(recipientAddress);
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
            List<SolanaTransactionSignatureVo> batch = solanaClient.getSignaturesForAddress(address, batchSize, beforeSignature);
            if (batch == null || batch.isEmpty()) {
                break;
            }

            for (SolanaTransactionSignatureVo signatureVo : batch) {
                if (lastSeenSignature != null && lastSeenSignature.equals(signatureVo.getSignature())) {
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
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setTxHash(transactionDetail.getSignature());
        paymentTransaction.setReferenceKey(transactionDetail.getPrimaryReferenceKey());
        paymentTransaction.setPayerAddress(transactionDetail.getPayerAddress());
        paymentTransaction.setRecipientAddress(transactionDetail.getRecipientAddress());
        paymentTransaction.setAmount(transactionDetail.getAmount());
        paymentTransaction.setCurrency(resolveCurrency(transactionDetail.getMintAddress()));
        paymentTransaction.setMintAddress(transactionDetail.getMintAddress());
        paymentTransaction.setBlockTime(transactionDetail.getBlockTime());
        paymentTransaction.setVerificationResult(VERIFICATION_RESULT_PENDING);
        paymentTransaction.setPaymentStatus(PAYMENT_STATUS_DETECTED);
        paymentTransaction.setRawPayload(parseRawPayload(transactionDetail.getRawPayload()));
        return paymentTransaction;
    }

    private String resolveCurrency(String mintAddress) {
        if (mintAddress == null || mintAddress.isBlank()) {
            return DEFAULT_CURRENCY_UNKNOWN;
        }
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
