package com.stableflow.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import com.stableflow.verification.vo.PaymentVerificationResultVo;
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
class PaymentVerificationServiceTest {

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private InvoicePaymentRequestMapper invoicePaymentRequestMapper;

    @Mock
    private InvoiceService invoiceService;

    private SinglePaymentVerificationService singlePaymentVerificationService;
    private PaymentVerificationService paymentVerificationService;

    @BeforeEach
    void setUp() {
        singlePaymentVerificationService = new SinglePaymentVerificationServiceImpl(
            paymentTransactionService,
            invoicePaymentRequestMapper,
            invoiceService
        );
        paymentVerificationService = new PaymentVerificationServiceImpl(
            paymentTransactionService,
            singlePaymentVerificationService
        );
    }

    @Test
    void shouldMarkTransactionAsMissingReference() {
        PaymentTransaction paymentTransaction = transaction(1L, "tx-1", null, "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.MISSING_REFERENCE, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.UNMATCHED, result.paymentStatus());
        verify(paymentTransactionService).updateById(any(PaymentTransaction.class));
    }

    @Test
    void shouldTreatBlankReferenceAsMissingReference() {
        PaymentTransaction paymentTransaction = transaction(21L, "tx-21", "   ", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.MISSING_REFERENCE, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.UNMATCHED, result.paymentStatus());
        assertEquals(null, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsInvalidReference() {
        PaymentTransaction paymentTransaction = transaction(2L, "tx-2", "ref-missing", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(null);

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.INVALID_REFERENCE, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.UNMATCHED, result.paymentStatus());
        assertEquals(null, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsInvalidReferenceWhenInvoiceBehindReferenceIsMissing() {
        PaymentTransaction paymentTransaction = transaction(22L, "tx-22", "ref-missing-invoice", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(
            paymentRequest(122L, "ref-missing-invoice", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z"))
        );
        when(invoiceService.getById(122L)).thenReturn(null);

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.INVALID_REFERENCE, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.UNMATCHED, result.paymentStatus());
        assertEquals(null, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsWrongCurrency() {
        PaymentTransaction paymentTransaction = transaction(3L, "tx-3", "ref-1", "10.00", "wrong-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(100L, "ref-1", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(100L)).thenReturn(invoice(100L));

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.WRONG_CURRENCY, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.UNMATCHED, result.paymentStatus());
        assertEquals(100L, result.invoiceId());
    }

    @Test
    void shouldKeepTransactionPendingWhenInvoiceIsDraft() {
        PaymentTransaction paymentTransaction = transaction(31L, "tx-31", "ref-draft", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(131L, "ref-draft", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(131L)).thenReturn(invoice(131L, InvoiceStatusEnum.DRAFT));

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.PENDING, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.UNMATCHED, result.paymentStatus());
        assertEquals(null, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsPaidWhenAmountMatches() {
        PaymentTransaction paymentTransaction = transaction(4L, "tx-4", "ref-2", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(101L, "ref-2", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(101L)).thenReturn(invoice(101L));
        when(paymentTransactionService.list(anyWrapper())).thenReturn(List.of());

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.PAID, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.PAID, result.paymentStatus());
        assertEquals(101L, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsPartiallyPaidWhenAmountIsLower() {
        PaymentTransaction paymentTransaction = transaction(5L, "tx-5", "ref-3", "8.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(102L, "ref-3", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(102L)).thenReturn(invoice(102L));
        when(paymentTransactionService.list(anyWrapper())).thenReturn(List.of());

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.PARTIALLY_PAID, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.PARTIALLY_PAID, result.paymentStatus());
    }

    @Test
    void shouldMarkTransactionAsOverpaidWhenAmountIsGreater() {
        PaymentTransaction paymentTransaction = transaction(6L, "tx-6", "ref-4", "12.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(103L, "ref-4", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(103L)).thenReturn(invoice(103L));
        when(paymentTransactionService.list(anyWrapper())).thenReturn(List.of());

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.OVERPAID, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.OVERPAID, result.paymentStatus());
    }

    @Test
    void shouldMarkTransactionAsLatePaymentWhenBlockTimeIsAfterExpiry() {
        PaymentTransaction paymentTransaction = transaction(7L, "tx-7", "ref-5", "10.00", "usdc-mint", utc("2026-03-18T10:00:01Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(104L, "ref-5", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(104L)).thenReturn(invoice(104L));

        PaymentVerificationResultVo result = singlePaymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.LATE_PAYMENT, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.EXPIRED, result.paymentStatus());
    }

    @Test
    void shouldMarkTransactionAsDuplicateWhenEarlierEffectivePaymentExists() {
        PaymentTransaction paymentTransaction = transaction(8L, "tx-8", "ref-6", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        PaymentTransaction earlierTransaction = transaction(7L, "tx-7", "ref-6", "10.00", "usdc-mint", utc("2026-03-17T09:59:00Z"));
        earlierTransaction.setInvoiceId(105L);
        earlierTransaction.setVerificationResult(PaymentVerificationResultEnum.PAID);

        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(105L, "ref-6", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(105L)).thenReturn(invoice(105L));
        when(paymentTransactionService.list(anyWrapper())).thenReturn(List.of(earlierTransaction));

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.DUPLICATE_PAYMENT, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.DUPLICATE, result.paymentStatus());
    }

    @Test
    void shouldMarkTransactionAsDuplicateWhenSameBlockTimeUsesLowerIdAsTieBreaker() {
        PaymentTransaction paymentTransaction = transaction(81L, "tx-81", "ref-81", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        PaymentTransaction earlierTransaction = transaction(80L, "tx-80", "ref-81", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        earlierTransaction.setInvoiceId(181L);
        earlierTransaction.setVerificationResult(PaymentVerificationResultEnum.PAID);

        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(181L, "ref-81", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(181L)).thenReturn(invoice(181L));
        when(paymentTransactionService.list(anyWrapper())).thenReturn(List.of(earlierTransaction));

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals(PaymentVerificationResultEnum.DUPLICATE_PAYMENT, result.verificationResult());
        assertEquals(PaymentTransactionStatusEnum.DUPLICATE, result.paymentStatus());
    }

    @Test
    void shouldVerifyPendingTransactionsInBatch() {
        PaymentTransaction firstTransaction = transaction(9L, "tx-9", null, "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        PaymentTransaction secondTransaction = transaction(10L, "tx-10", "ref-7", "10.00", "usdc-mint", utc("2026-03-17T10:01:00Z"));

        when(paymentTransactionService.listPendingVerificationTransactions(20)).thenReturn(List.of(firstTransaction, secondTransaction));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(106L, "ref-7", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(106L)).thenReturn(invoice(106L));
        when(paymentTransactionService.list(anyWrapper())).thenReturn(List.of());

        int processedCount = paymentVerificationService.verifyPendingTransactions(20);

        assertEquals(2, processedCount);
        verify(paymentTransactionService, org.mockito.Mockito.times(2)).updateById(any(PaymentTransaction.class));
    }

    @Test
    void shouldContinueBatchVerificationWhenSingleTransactionFails() {
        PaymentTransaction failedTransaction = transaction(11L, "tx-11", "ref-8", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        failedTransaction.setAmount(null);
        PaymentTransaction successTransaction = transaction(12L, "tx-12", "ref-9", "10.00", "usdc-mint", utc("2026-03-17T10:01:00Z"));

        when(paymentTransactionService.listPendingVerificationTransactions(10)).thenReturn(List.of(failedTransaction, successTransaction));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(
            paymentRequest(107L, "ref-8", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")),
            paymentRequest(108L, "ref-9", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z"))
        );
        when(invoiceService.getById(107L)).thenReturn(invoice(107L));
        when(invoiceService.getById(108L)).thenReturn(invoice(108L));
        when(paymentTransactionService.list(anyWrapper())).thenReturn(List.of());

        int processedCount = paymentVerificationService.verifyPendingTransactions(10);

        assertEquals(1, processedCount);
        verify(paymentTransactionService).updateById(any(PaymentTransaction.class));
    }

    private PaymentTransaction transaction(
        Long id,
        String txHash,
        String referenceKey,
        String amount,
        String mintAddress,
        OffsetDateTime blockTime
    ) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setId(id);
        paymentTransaction.setTxHash(txHash);
        paymentTransaction.setReferenceKey(referenceKey);
        paymentTransaction.setAmount(new BigDecimal(amount));
        paymentTransaction.setMintAddress(mintAddress);
        paymentTransaction.setBlockTime(blockTime);
        return paymentTransaction;
    }

    private InvoicePaymentRequest paymentRequest(
        Long invoiceId,
        String referenceKey,
        String mintAddress,
        String expectedAmount,
        OffsetDateTime expireAt
    ) {
        InvoicePaymentRequest paymentRequest = new InvoicePaymentRequest();
        paymentRequest.setInvoiceId(invoiceId);
        paymentRequest.setReferenceKey(referenceKey);
        paymentRequest.setMintAddress(mintAddress);
        paymentRequest.setExpectedAmount(new BigDecimal(expectedAmount));
        paymentRequest.setExpireAt(expireAt);
        return paymentRequest;
    }

    private Invoice invoice(Long id) {
        return invoice(id, InvoiceStatusEnum.PENDING);
    }

    private Invoice invoice(Long id, InvoiceStatusEnum status) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setStatus(status);
        return invoice;
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<PaymentTransaction> anyWrapper() {
        return any(Wrapper.class);
    }
}
