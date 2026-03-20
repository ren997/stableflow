package com.stableflow.invoice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.invoice.mapper.InvoicePaymentRequestMapper;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.service.ReconciliationRecordService;
import com.stableflow.system.config.PaymentProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private InvoicePaymentRequestMapper invoicePaymentRequestMapper;

    @Mock
    private MerchantPaymentConfigService merchantPaymentConfigService;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private ReconciliationRecordService reconciliationRecordService;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(
            invoiceMapper,
            invoicePaymentRequestMapper,
            merchantPaymentConfigService,
            currentMerchantProvider,
            new PaymentProperties("fixed-address-reference", "https://stableflow.test"),
            paymentTransactionService,
            reconciliationRecordService
        );
    }

    @Test
    void shouldReturnInvoiceStatusWhenNoTransactionExists() {
        Invoice invoice = invoice(100L, 10L, InvoiceStatusEnum.PENDING, null, null);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(100L)).thenReturn(invoice);
        when(paymentTransactionService.getLatestTransactionByInvoiceId(100L)).thenReturn(null);
        when(reconciliationRecordService.getLatestRecordByInvoiceId(100L)).thenReturn(null);

        PaymentStatusVo paymentStatus = invoiceService.getPaymentStatus(100L);

        assertEquals(InvoiceStatusEnum.PENDING, paymentStatus.status());
        assertEquals(List.of(), paymentStatus.exceptionTags());
        assertEquals(null, paymentStatus.latestTxHash());
        assertEquals(null, paymentStatus.lastProcessedAt());
    }

    @Test
    void shouldFallbackLastProcessedAtToLatestTransactionBlockTime() {
        Invoice invoice = invoice(101L, 10L, InvoiceStatusEnum.PENDING, List.of("WRONG_CURRENCY"), null);
        PaymentTransaction latestTransaction = paymentTransaction(
            101L,
            "tx-latest",
            PaymentVerificationResultEnum.WRONG_CURRENCY,
            PaymentTransactionStatusEnum.UNMATCHED,
            utc("2026-03-18T10:00:00Z")
        );

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(101L)).thenReturn(invoice);
        when(paymentTransactionService.getLatestTransactionByInvoiceId(101L)).thenReturn(latestTransaction);
        when(reconciliationRecordService.getLatestRecordByInvoiceId(101L)).thenReturn(null);

        PaymentStatusVo paymentStatus = invoiceService.getPaymentStatus(101L);

        assertEquals(List.of(ExceptionTagEnum.WRONG_CURRENCY), paymentStatus.exceptionTags());
        assertEquals("tx-latest", paymentStatus.latestTxHash());
        assertEquals(PaymentVerificationResultEnum.WRONG_CURRENCY, paymentStatus.latestVerificationResult());
        assertEquals(PaymentTransactionStatusEnum.UNMATCHED, paymentStatus.latestPaymentStatus());
        assertEquals(utc("2026-03-18T10:00:00Z"), paymentStatus.lastProcessedAt());
    }

    @Test
    void shouldUseLatestReconciliationProcessedTimeWhenExists() {
        Invoice invoice = invoice(102L, 10L, InvoiceStatusEnum.PAID, List.of("LATE_PAYMENT", "DUPLICATE_PAYMENT"), utc("2026-03-18T09:58:00Z"));
        PaymentTransaction latestTransaction = paymentTransaction(
            102L,
            "tx-paid",
            PaymentVerificationResultEnum.PAID,
            PaymentTransactionStatusEnum.PAID,
            utc("2026-03-18T10:00:00Z")
        );
        ReconciliationRecord reconciliationRecord = reconciliationRecord(102L, "tx-paid", utc("2026-03-18T10:05:00Z"));

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(102L)).thenReturn(invoice);
        when(paymentTransactionService.getLatestTransactionByInvoiceId(102L)).thenReturn(latestTransaction);
        when(reconciliationRecordService.getLatestRecordByInvoiceId(102L)).thenReturn(reconciliationRecord);

        PaymentStatusVo paymentStatus = invoiceService.getPaymentStatus(102L);

        assertEquals(InvoiceStatusEnum.PAID, paymentStatus.status());
        assertEquals(List.of(ExceptionTagEnum.LATE_PAYMENT, ExceptionTagEnum.DUPLICATE_PAYMENT), paymentStatus.exceptionTags());
        assertEquals(utc("2026-03-18T09:58:00Z"), paymentStatus.paidAt());
        assertEquals(utc("2026-03-18T10:05:00Z"), paymentStatus.lastProcessedAt());
    }

    @Test
    void shouldReturnLatestTransactionContextFromService() {
        Invoice invoice = invoice(103L, 10L, InvoiceStatusEnum.OVERPAID, null, null);
        PaymentTransaction latestTransaction = paymentTransaction(
            103L,
            "tx-over",
            PaymentVerificationResultEnum.OVERPAID,
            PaymentTransactionStatusEnum.OVERPAID,
            utc("2026-03-18T11:00:00Z")
        );

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(103L)).thenReturn(invoice);
        when(paymentTransactionService.getLatestTransactionByInvoiceId(103L)).thenReturn(latestTransaction);
        when(reconciliationRecordService.getLatestRecordByInvoiceId(103L)).thenReturn(null);

        PaymentStatusVo paymentStatus = invoiceService.getPaymentStatus(103L);

        assertEquals("tx-over", paymentStatus.latestTxHash());
        assertEquals(PaymentVerificationResultEnum.OVERPAID, paymentStatus.latestVerificationResult());
        assertEquals(PaymentTransactionStatusEnum.OVERPAID, paymentStatus.latestPaymentStatus());
    }

    @Test
    void shouldRejectInvoiceNotOwnedByCurrentMerchant() {
        Invoice invoice = invoice(104L, 11L, InvoiceStatusEnum.PENDING, null, null);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(104L)).thenReturn(invoice);

        BusinessException exception = assertThrows(BusinessException.class, () -> invoiceService.getPaymentStatus(104L));

        assertEquals(ErrorCode.INVOICE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldApplyStatusAndExceptionTagFiltersWhenListingInvoices() {
        Invoice invoice = invoice(105L, 10L, InvoiceStatusEnum.EXPIRED, List.of("LATE_PAYMENT"), null);
        Page<Invoice> page = new Page<>(2, 10);
        page.setRecords(List.of(invoice));
        page.setTotal(1L);

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var response = invoiceService.listInvoices("EXPIRED", ExceptionTagEnum.LATE_PAYMENT, 2, 10);

        assertEquals(1L, response.total());
        assertEquals(1, response.records().size());
        assertEquals("INV-105", response.records().get(0).invoiceNo());
        assertEquals(InvoiceStatusEnum.EXPIRED, response.records().get(0).status());
    }

    private Invoice invoice(
        Long id,
        Long merchantId,
        InvoiceStatusEnum status,
        List<String> exceptionTags,
        OffsetDateTime paidAt
    ) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setMerchantId(merchantId);
        invoice.setPublicId("pub_" + id);
        invoice.setInvoiceNo("INV-" + id);
        invoice.setAmount(new BigDecimal("99.00"));
        invoice.setCurrency("USDC");
        invoice.setStatus(status);
        invoice.setExceptionTags(exceptionTags);
        invoice.setPaidAt(paidAt);
        return invoice;
    }

    private PaymentTransaction paymentTransaction(
        Long invoiceId,
        String txHash,
        PaymentVerificationResultEnum verificationResult,
        PaymentTransactionStatusEnum paymentStatus,
        OffsetDateTime blockTime
    ) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setInvoiceId(invoiceId);
        paymentTransaction.setTxHash(txHash);
        paymentTransaction.setVerificationResult(verificationResult);
        paymentTransaction.setPaymentStatus(paymentStatus);
        paymentTransaction.setBlockTime(blockTime);
        return paymentTransaction;
    }

    private ReconciliationRecord reconciliationRecord(Long invoiceId, String txHash, OffsetDateTime processedAt) {
        ReconciliationRecord reconciliationRecord = new ReconciliationRecord();
        reconciliationRecord.setInvoiceId(invoiceId);
        reconciliationRecord.setTxHash(txHash);
        reconciliationRecord.setProcessedAt(processedAt);
        return reconciliationRecord;
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value);
    }
}
