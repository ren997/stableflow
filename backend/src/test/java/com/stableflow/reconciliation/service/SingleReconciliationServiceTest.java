package com.stableflow.reconciliation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.outbox.service.OutboxEventService;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.enums.ReconciliationStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleReconciliationServiceTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private ReconciliationRecordService reconciliationRecordService;

    @Mock
    private PaymentProofService paymentProofService;

    @Mock
    private OutboxEventService outboxEventService;

    private SingleReconciliationService singleReconciliationService;

    @BeforeEach
    void setUp() {
        singleReconciliationService = new SingleReconciliationServiceImpl(
            invoiceService,
            reconciliationRecordService,
            paymentProofService,
            outboxEventService,
            new ObjectMapper()
        );
    }

    @Test
    void shouldMarkInvoiceAsPaidAndCreateReconciliationRecord() {
        PaymentTransaction paymentTransaction = transaction(100L, "tx-paid", PaymentVerificationResultEnum.PAID, utc("2026-03-17T10:00:00Z"));
        Invoice invoice = invoice(100L, InvoiceStatusEnum.PENDING, null);

        when(reconciliationRecordService.existsByInvoiceIdAndTxHash(100L, "tx-paid")).thenReturn(false);
        when(invoiceService.getById(100L)).thenReturn(invoice);
        when(reconciliationRecordService.saveIfAbsent(any(ReconciliationRecord.class))).thenReturn(true);
        when(outboxEventService.saveInvoicePaymentResultEvent(any(), any(), any(), any(), any(), any())).thenReturn(true);

        boolean reconciled = singleReconciliationService.reconcileTransaction(paymentTransaction);

        assertEquals(true, reconciled);
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceService).updateById(invoiceCaptor.capture());
        assertEquals(InvoiceStatusEnum.PAID, invoiceCaptor.getValue().getStatus());
        assertEquals(utc("2026-03-17T10:00:00Z"), invoiceCaptor.getValue().getPaidAt());

        ArgumentCaptor<ReconciliationRecord> recordCaptor = ArgumentCaptor.forClass(ReconciliationRecord.class);
        verify(reconciliationRecordService).saveIfAbsent(recordCaptor.capture());
        assertEquals(ReconciliationStatusEnum.SUCCESS, recordCaptor.getValue().getReconciliationStatus());
        assertEquals("Invoice marked as paid.", recordCaptor.getValue().getResultMessage());
        verify(paymentProofService).saveIfAbsent(any(), any(), any(), any(), any(), any());
        verify(outboxEventService).saveInvoicePaymentResultEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldKeepInvoiceStatusAndAppendExceptionTagForWrongCurrency() {
        PaymentTransaction paymentTransaction = transaction(101L, "tx-wrong", PaymentVerificationResultEnum.WRONG_CURRENCY, utc("2026-03-17T10:00:00Z"));
        Invoice invoice = invoice(101L, InvoiceStatusEnum.PENDING, null);

        when(reconciliationRecordService.existsByInvoiceIdAndTxHash(101L, "tx-wrong")).thenReturn(false);
        when(invoiceService.getById(101L)).thenReturn(invoice);
        when(reconciliationRecordService.saveIfAbsent(any(ReconciliationRecord.class))).thenReturn(true);
        when(outboxEventService.saveInvoicePaymentResultEvent(any(), any(), any(), any(), any(), any())).thenReturn(true);

        boolean reconciled = singleReconciliationService.reconcileTransaction(paymentTransaction);

        assertEquals(true, reconciled);
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceService).updateById(invoiceCaptor.capture());
        assertEquals(InvoiceStatusEnum.PENDING, invoiceCaptor.getValue().getStatus());
        assertEquals(List.of("WRONG_CURRENCY"), invoiceCaptor.getValue().getExceptionTags());

        ArgumentCaptor<ReconciliationRecord> recordCaptor = ArgumentCaptor.forClass(ReconciliationRecord.class);
        verify(reconciliationRecordService).saveIfAbsent(recordCaptor.capture());
        assertEquals(ReconciliationStatusEnum.SKIPPED, recordCaptor.getValue().getReconciliationStatus());
        verify(paymentProofService).saveIfAbsent(any(), any(), any(), any(), any(), any());
        verify(outboxEventService).saveInvoicePaymentResultEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldDeduplicateExistingAndIncomingExceptionTags() {
        PaymentTransaction paymentTransaction = transaction(103L, "tx-late", PaymentVerificationResultEnum.LATE_PAYMENT, utc("2026-03-17T10:00:00Z"));
        Invoice invoice = invoice(103L, InvoiceStatusEnum.EXPIRED, List.of("LATE_PAYMENT", " ", "WRONG_CURRENCY"));

        when(reconciliationRecordService.existsByInvoiceIdAndTxHash(103L, "tx-late")).thenReturn(false);
        when(invoiceService.getById(103L)).thenReturn(invoice);
        when(reconciliationRecordService.saveIfAbsent(any(ReconciliationRecord.class))).thenReturn(true);
        when(outboxEventService.saveInvoicePaymentResultEvent(any(), any(), any(), any(), any(), any())).thenReturn(true);

        boolean reconciled = singleReconciliationService.reconcileTransaction(paymentTransaction);

        assertEquals(true, reconciled);
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceService).updateById(invoiceCaptor.capture());
        assertEquals(List.of("LATE_PAYMENT", "WRONG_CURRENCY"), invoiceCaptor.getValue().getExceptionTags());
    }

    @Test
    void shouldSkipWhenReconciliationAlreadyExists() {
        PaymentTransaction paymentTransaction = transaction(102L, "tx-dup", PaymentVerificationResultEnum.PAID, utc("2026-03-17T10:00:00Z"));
        when(reconciliationRecordService.existsByInvoiceIdAndTxHash(102L, "tx-dup")).thenReturn(true);

        boolean reconciled = singleReconciliationService.reconcileTransaction(paymentTransaction);

        assertFalse(reconciled);
    }

    private PaymentTransaction transaction(
        Long invoiceId,
        String txHash,
        PaymentVerificationResultEnum verificationResult,
        OffsetDateTime blockTime
    ) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setInvoiceId(invoiceId);
        paymentTransaction.setTxHash(txHash);
        paymentTransaction.setVerificationResult(verificationResult);
        paymentTransaction.setBlockTime(blockTime);
        return paymentTransaction;
    }

    private Invoice invoice(Long id, InvoiceStatusEnum status, List<String> exceptionTags) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setStatus(status);
        invoice.setExceptionTags(exceptionTags);
        return invoice;
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value);
    }
}
