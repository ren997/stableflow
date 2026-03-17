package com.stableflow.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.entity.InvoicePaymentRequest;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
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

    private PaymentVerificationService paymentVerificationService;

    @BeforeEach
    void setUp() {
        paymentVerificationService = new PaymentVerificationServiceImpl(
            paymentTransactionService,
            invoicePaymentRequestMapper,
            invoiceService
        );
    }

    @Test
    void shouldMarkTransactionAsMissingReference() {
        PaymentTransaction paymentTransaction = transaction(1L, "tx-1", null, "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("MISSING_REFERENCE", result.verificationResult());
        assertEquals("UNMATCHED", result.paymentStatus());
        verify(paymentTransactionService).updateById(any(PaymentTransaction.class));
    }

    @Test
    void shouldMarkTransactionAsInvalidReference() {
        PaymentTransaction paymentTransaction = transaction(2L, "tx-2", "ref-missing", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(null);

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("INVALID_REFERENCE", result.verificationResult());
        assertEquals("UNMATCHED", result.paymentStatus());
        assertEquals(null, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsWrongCurrency() {
        PaymentTransaction paymentTransaction = transaction(3L, "tx-3", "ref-1", "10.00", "wrong-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(100L, "ref-1", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(100L)).thenReturn(invoice(100L));

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("WRONG_CURRENCY", result.verificationResult());
        assertEquals("UNMATCHED", result.paymentStatus());
        assertEquals(100L, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsPaidWhenAmountMatches() {
        PaymentTransaction paymentTransaction = transaction(4L, "tx-4", "ref-2", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(101L, "ref-2", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(101L)).thenReturn(invoice(101L));
        when(paymentTransactionService.list(any())).thenReturn(List.of());

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("PAID", result.verificationResult());
        assertEquals("PAID", result.paymentStatus());
        assertEquals(101L, result.invoiceId());
    }

    @Test
    void shouldMarkTransactionAsPartiallyPaidWhenAmountIsLower() {
        PaymentTransaction paymentTransaction = transaction(5L, "tx-5", "ref-3", "8.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(102L, "ref-3", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(102L)).thenReturn(invoice(102L));
        when(paymentTransactionService.list(any())).thenReturn(List.of());

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("PARTIALLY_PAID", result.verificationResult());
        assertEquals("PARTIALLY_PAID", result.paymentStatus());
    }

    @Test
    void shouldMarkTransactionAsOverpaidWhenAmountIsGreater() {
        PaymentTransaction paymentTransaction = transaction(6L, "tx-6", "ref-4", "12.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(103L, "ref-4", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(103L)).thenReturn(invoice(103L));
        when(paymentTransactionService.list(any())).thenReturn(List.of());

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("OVERPAID", result.verificationResult());
        assertEquals("OVERPAID", result.paymentStatus());
    }

    @Test
    void shouldMarkTransactionAsLatePaymentWhenBlockTimeIsAfterExpiry() {
        PaymentTransaction paymentTransaction = transaction(7L, "tx-7", "ref-5", "10.00", "usdc-mint", utc("2026-03-18T10:00:01Z"));
        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(104L, "ref-5", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(104L)).thenReturn(invoice(104L));

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("LATE_PAYMENT", result.verificationResult());
        assertEquals("EXPIRED", result.paymentStatus());
    }

    @Test
    void shouldMarkTransactionAsDuplicateWhenEarlierEffectivePaymentExists() {
        PaymentTransaction paymentTransaction = transaction(8L, "tx-8", "ref-6", "10.00", "usdc-mint", utc("2026-03-17T10:00:00Z"));
        PaymentTransaction earlierTransaction = transaction(7L, "tx-7", "ref-6", "10.00", "usdc-mint", utc("2026-03-17T09:59:00Z"));
        earlierTransaction.setInvoiceId(105L);
        earlierTransaction.setVerificationResult("PAID");

        when(invoicePaymentRequestMapper.selectOne(any())).thenReturn(paymentRequest(105L, "ref-6", "usdc-mint", "10.00", utc("2026-03-18T10:00:00Z")));
        when(invoiceService.getById(105L)).thenReturn(invoice(105L));
        when(paymentTransactionService.list(any())).thenReturn(List.of(earlierTransaction));

        PaymentVerificationResultVo result = paymentVerificationService.verifyTransaction(paymentTransaction);

        assertEquals("DUPLICATE_PAYMENT", result.verificationResult());
        assertEquals("DUPLICATE", result.paymentStatus());
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
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setStatus("PENDING");
        return invoice;
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
