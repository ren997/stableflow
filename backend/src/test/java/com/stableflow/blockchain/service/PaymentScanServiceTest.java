package com.stableflow.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.client.SolanaClient;
import com.stableflow.blockchain.entity.PaymentScanCursor;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.blockchain.vo.SolanaTransactionSignatureVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.config.SolanaScanProperties;
import com.stableflow.system.enums.SolanaNetworkEnum;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentScanServiceTest {

    @Mock
    private SolanaClient solanaClient;

    @Mock
    private MerchantPaymentConfigService merchantPaymentConfigService;

    @Mock
    private PaymentScanCursorService paymentScanCursorService;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    private final SolanaProperties solanaProperties = new SolanaProperties(
        SolanaNetworkEnum.DEVNET,
        "http://localhost:8899",
        "usdc-mint-1",
        java.time.Duration.ofSeconds(3),
        java.time.Duration.ofSeconds(10),
        3,
        java.time.Duration.ofMillis(500)
    );

    private final SolanaScanProperties solanaScanProperties = new SolanaScanProperties(
        true,
        2,
        30_000L,
        10_000L,
        true,
        "stableflow:job:payment-scan:lock",
        java.time.Duration.ofMinutes(5)
    );

    private PaymentScanService paymentScanService;

    @BeforeEach
    void setUp() {
        paymentScanService = new PaymentScanServiceImpl(
            solanaClient,
            merchantPaymentConfigService,
            paymentScanCursorService,
            paymentTransactionService,
            solanaProperties,
            solanaScanProperties,
            new ObjectMapper()
        );
    }

    @Test
    void shouldPageUntilExistingCursorAndPersistOldestFirst() {
        MerchantPaymentConfig paymentConfig = new MerchantPaymentConfig();
        paymentConfig.setWalletAddress("merchant-wallet-1");

        PaymentScanCursor cursor = new PaymentScanCursor();
        cursor.setRecipientAddress("merchant-wallet-1");
        cursor.setLastSeenSignature("sig-known");

        when(paymentScanCursorService.getOrCreate("merchant-wallet-1")).thenReturn(cursor);
        when(solanaClient.getSignaturesForAddress("merchant-wallet-1", 2, null)).thenReturn(
            List.of(signature("sig-3"), signature("sig-2"))
        );
        when(solanaClient.getSignaturesForAddress("merchant-wallet-1", 2, "sig-2")).thenReturn(
            List.of(signature("sig-1"), signature("sig-known"))
        );
        when(solanaClient.getTransaction("sig-1")).thenReturn(detail("sig-1", "ref-1", "usdc-mint-1", "{\"slot\":1}"));
        when(solanaClient.getTransaction("sig-2")).thenReturn(detail("sig-2", "ref-2", "usdc-mint-1", "{\"slot\":2}"));
        when(solanaClient.getTransaction("sig-3")).thenReturn(detail("sig-3", "ref-3", "wrong-mint", "{\"slot\":3}"));
        when(paymentTransactionService.saveIfAbsent(any(PaymentTransaction.class))).thenReturn(true);

        int insertedCount = paymentScanService.scanRecipientAddress(paymentConfig);

        assertEquals(3, insertedCount);
        verify(paymentScanCursorService).updateCursor("merchant-wallet-1", "sig-3");

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionService, org.mockito.Mockito.times(3)).saveIfAbsent(transactionCaptor.capture());
        List<PaymentTransaction> transactions = transactionCaptor.getAllValues();
        assertEquals(List.of("sig-1", "sig-2", "sig-3"), transactions.stream().map(PaymentTransaction::getTxHash).toList());
        assertEquals("USDC", transactions.get(0).getCurrency());
        assertEquals("UNKNOWN", transactions.get(2).getCurrency());

        InOrder inOrder = inOrder(solanaClient);
        inOrder.verify(solanaClient).getTransaction("sig-1");
        inOrder.verify(solanaClient).getTransaction("sig-2");
        inOrder.verify(solanaClient).getTransaction("sig-3");
    }

    @Test
    void shouldTouchCursorWhenNoNewSignatureFound() {
        MerchantPaymentConfig paymentConfig = new MerchantPaymentConfig();
        paymentConfig.setWalletAddress("merchant-wallet-2");

        PaymentScanCursor cursor = new PaymentScanCursor();
        cursor.setRecipientAddress("merchant-wallet-2");
        cursor.setLastSeenSignature("sig-current");

        when(paymentScanCursorService.getOrCreate("merchant-wallet-2")).thenReturn(cursor);
        when(solanaClient.getSignaturesForAddress("merchant-wallet-2", 2, null)).thenReturn(
            List.of(signature("sig-current"))
        );

        int insertedCount = paymentScanService.scanRecipientAddress(paymentConfig);

        assertEquals(0, insertedCount);
        verify(paymentScanCursorService).updateCursor("merchant-wallet-2", "sig-current");
    }

    @Test
    void shouldSkipFailedAddressAndContinueScanningRemainingAddresses() {
        MerchantPaymentConfig failedConfig = new MerchantPaymentConfig();
        failedConfig.setMerchantId(1L);
        failedConfig.setWalletAddress("merchant-wallet-failed");

        MerchantPaymentConfig successfulConfig = new MerchantPaymentConfig();
        successfulConfig.setMerchantId(2L);
        successfulConfig.setWalletAddress("merchant-wallet-success");

        PaymentScanCursor failedCursor = new PaymentScanCursor();
        failedCursor.setRecipientAddress("merchant-wallet-failed");

        PaymentScanCursor successCursor = new PaymentScanCursor();
        successCursor.setRecipientAddress("merchant-wallet-success");

        when(merchantPaymentConfigService.listActiveConfigs()).thenReturn(List.of(failedConfig, successfulConfig));
        when(paymentScanCursorService.getOrCreate("merchant-wallet-failed")).thenReturn(failedCursor);
        when(paymentScanCursorService.getOrCreate("merchant-wallet-success")).thenReturn(successCursor);
        when(solanaClient.getSignaturesForAddress("merchant-wallet-failed", 2, null)).thenThrow(
            new BusinessException(ErrorCode.BLOCKCHAIN_RPC_TIMEOUT, "timeout")
        );
        when(solanaClient.getSignaturesForAddress("merchant-wallet-success", 2, null)).thenReturn(
            List.of(signature("sig-success"))
        );
        when(solanaClient.getTransaction("sig-success")).thenReturn(
            detail("sig-success", "ref-success", "usdc-mint-1", "{\"slot\":99}")
        );
        when(paymentTransactionService.saveIfAbsent(any(PaymentTransaction.class))).thenReturn(true);

        int insertedCount = paymentScanService.scanAllActiveAddresses();

        assertEquals(1, insertedCount);
        verify(paymentScanCursorService, never()).updateCursor("merchant-wallet-failed", null);
        verify(paymentScanCursorService).updateCursor("merchant-wallet-success", "sig-success");
    }

    private SolanaTransactionSignatureVo signature(String signature) {
        SolanaTransactionSignatureVo signatureVo = new SolanaTransactionSignatureVo();
        signatureVo.setSignature(signature);
        return signatureVo;
    }

    private SolanaTransactionDetailVo detail(String signature, String reference, String mintAddress, String rawPayload) {
        SolanaTransactionDetailVo detailVo = new SolanaTransactionDetailVo();
        detailVo.setSignature(signature);
        detailVo.setPrimaryReferenceKey(reference);
        detailVo.setPayerAddress("payer-" + signature);
        detailVo.setRecipientAddress("recipient-" + signature);
        detailVo.setAmount(new BigDecimal("10.50"));
        detailVo.setMintAddress(mintAddress);
        detailVo.setBlockTime(OffsetDateTime.now());
        detailVo.setRawPayload(rawPayload);
        return detailVo;
    }
}
