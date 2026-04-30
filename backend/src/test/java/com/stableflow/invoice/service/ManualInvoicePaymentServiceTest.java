package com.stableflow.invoice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.client.SolanaClient;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.blockchain.vo.SolanaTransactionDetailVo;
import com.stableflow.invoice.dto.ManualSubmitPaymentRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.vo.ManualSubmitPaymentVo;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.reconciliation.service.SingleReconciliationService;
import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.enums.SolanaNetworkEnum;
import com.stableflow.system.security.CurrentMerchantProvider;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManualInvoicePaymentServiceTest {

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private InvoicePaymentRequestMapper invoicePaymentRequestMapper;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    @Mock
    private SolanaClient solanaClient;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private SingleReconciliationService singleReconciliationService;

    private ManualInvoicePaymentService manualInvoicePaymentService;

    @BeforeEach
    void setUp() {
        manualInvoicePaymentService = new ManualInvoicePaymentServiceImpl(
            invoiceMapper,
            invoicePaymentRequestMapper,
            currentMerchantProvider,
            solanaClient,
            paymentTransactionService,
            invoiceService,
            singleReconciliationService,
            new SolanaProperties(
                SolanaNetworkEnum.MAINNET,
                "https://mainnet.helius.dev",
                "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                java.time.Duration.ofSeconds(3),
                java.time.Duration.ofSeconds(10),
                3,
                java.time.Duration.ofMillis(500)
            ),
            new ObjectMapper()
        );
    }

    @Test
    void shouldManuallyMatchAtaPaymentWithoutReferenceAndReconcileInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(100L);
        invoice.setMerchantId(7L);
        invoice.setStatus(InvoiceStatusEnum.PENDING);

        InvoicePaymentRequest paymentRequest = new InvoicePaymentRequest();
        paymentRequest.setInvoiceId(100L);
        paymentRequest.setRecipientAddress("9qtfKWYQUVXem2uZ9No4ZCZLCEmNDgA6Tk3u3bsbLwZv");
        paymentRequest.setReferenceKey("invoice-ref-100");
        paymentRequest.setMintAddress("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v");
        paymentRequest.setExpectedAmount(new BigDecimal("0.20"));
        paymentRequest.setExpireAt(utc("2026-04-18T10:00:00Z"));

        SolanaTransactionDetailVo transactionDetail = new SolanaTransactionDetailVo();
        transactionDetail.setSignature("tx-manual-100");
        transactionDetail.setPayerAddress("payer-1");
        transactionDetail.setRecipientAddress("8wJqbV7Z1YCBo52nAcx1UQdtnNsE9jLVN5Nemuh64VrL");
        transactionDetail.setMintAddress("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v");
        transactionDetail.setAmount(new BigDecimal("0.20"));
        transactionDetail.setBlockTime(utc("2026-04-17T09:30:00Z"));
        transactionDetail.setSuccess(true);
        transactionDetail.setRawPayload("{\"slot\":1}");

        PaymentTransaction persistedTransaction = new PaymentTransaction();
        persistedTransaction.setId(11L);
        persistedTransaction.setTxHash("tx-manual-100");
        persistedTransaction.setRecipientAddress("8wJqbV7Z1YCBo52nAcx1UQdtnNsE9jLVN5Nemuh64VrL");
        persistedTransaction.setAmount(new BigDecimal("0.20"));
        persistedTransaction.setMintAddress("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v");
        persistedTransaction.setBlockTime(utc("2026-04-17T09:30:00Z"));

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(7L);
        when(invoiceMapper.selectById(100L)).thenReturn(invoice);
        when(invoicePaymentRequestMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paymentRequest);
        when(paymentTransactionService.getByTxHash("tx-manual-100")).thenReturn(null, persistedTransaction);
        when(solanaClient.getTransaction("tx-manual-100")).thenReturn(transactionDetail);
        when(paymentTransactionService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(singleReconciliationService.reconcileTransaction(any(PaymentTransaction.class))).thenReturn(true);
        when(invoiceService.getPaymentStatus(100L)).thenReturn(
            new PaymentStatusVo(
                100L,
                "pub-100",
                "INV-100",
                InvoiceStatusEnum.PAID,
                List.of(),
                utc("2026-04-17T09:30:00Z"),
                utc("2026-04-17T09:31:00Z"),
                "tx-manual-100",
                PaymentVerificationResultEnum.PAID,
                PaymentTransactionStatusEnum.PAID
            )
        );

        ManualSubmitPaymentVo result = manualInvoicePaymentService.submitPayment(
            new ManualSubmitPaymentRequestDto(100L, "tx-manual-100")
        );

        assertEquals(100L, result.invoiceId());
        assertEquals(11L, result.paymentTransactionId());
        assertEquals("tx-manual-100", result.txHash());
        assertNull(result.referenceKey());
        assertEquals(PaymentVerificationResultEnum.PAID, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.PAID, result.paymentTransactionStatus());
        assertEquals(1, result.reconciledCount());
        assertEquals(InvoiceStatusEnum.PAID, result.paymentStatus().status());
        verify(paymentTransactionService).saveIfAbsent(any(PaymentTransaction.class));
        verify(paymentTransactionService).updateById(any(PaymentTransaction.class));
        verify(singleReconciliationService).reconcileTransaction(any(PaymentTransaction.class));
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
