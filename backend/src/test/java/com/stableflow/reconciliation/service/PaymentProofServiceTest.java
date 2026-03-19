package com.stableflow.reconciliation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.mapper.InvoiceMapper;
import com.stableflow.reconciliation.entity.PaymentProof;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.enums.ReconciliationStatusEnum;
import com.stableflow.reconciliation.mapper.PaymentProofMapper;
import com.stableflow.reconciliation.vo.PaymentProofVo;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentProofServiceTest {

    @Mock
    private PaymentProofMapper paymentProofMapper;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    private PaymentProofService paymentProofService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        paymentProofService = new PaymentProofServiceImpl(
            paymentProofMapper,
            invoiceMapper,
            currentMerchantProvider,
            objectMapper
        );
    }

    @Test
    void shouldPersistPaymentProofSnapshotWhenAbsent() {
        Invoice invoice = invoice();
        PaymentTransaction paymentTransaction = paymentTransaction();
        ReconciliationRecord reconciliationRecord = reconciliationRecord();

        when(paymentProofMapper.selectCount(any())).thenReturn(0L);

        boolean saved = paymentProofService.saveIfAbsent(
            invoice,
            paymentTransaction,
            reconciliationRecord,
            InvoiceStatusEnum.PAID,
            List.of("LATE_PAYMENT", "DUPLICATE_PAYMENT"),
            utc("2026-03-17T10:00:00Z")
        );

        assertEquals(true, saved);
        ArgumentCaptor<PaymentProof> paymentProofCaptor = ArgumentCaptor.forClass(PaymentProof.class);
        verify(paymentProofMapper).insert(paymentProofCaptor.capture());
        PaymentProof savedProof = paymentProofCaptor.getValue();
        assertEquals(100L, savedProof.getInvoiceId());
        assertEquals("tx-paid", savedProof.getTxHash());
        assertEquals("INVOICE_PAYMENT_RESULT", savedProof.getProofType().getCode());
        assertEquals("ref_paid", savedProof.getProofPayload().get("referenceKey").asText());
        assertEquals("PAID", savedProof.getProofPayload().get("finalStatus").asText());
        assertEquals(2, savedProof.getProofPayload().get("exceptionTags").size());
    }

    @Test
    void shouldReturnLatestPaymentProofForOwnedInvoice() {
        Invoice invoice = invoice();
        PaymentProof paymentProof = new PaymentProof();
        paymentProof.setId(9L);
        paymentProof.setInvoiceId(100L);
        paymentProof.setTxHash("tx-paid");
        paymentProof.setCreatedAt(utc("2026-03-17T10:05:00Z"));
        paymentProof.setProofPayload(
            objectMapper.valueToTree(
                new TestPaymentProofPayload(
                    100L,
                    "pub_paid",
                    "INV-001",
                    "tx-paid",
                    "ref_paid",
                    "payer-1",
                    "recipient-1",
                    "mint-1",
                    new BigDecimal("99.00"),
                    utc("2026-03-17T10:00:00Z"),
                    PaymentVerificationResultEnum.PAID,
                    InvoiceStatusEnum.PAID,
                    List.of("LATE_PAYMENT"),
                    ReconciliationStatusEnum.SUCCESS,
                    "Invoice marked as paid."
                )
            )
        );

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(100L)).thenReturn(invoice);
        when(paymentProofMapper.selectOne(any())).thenReturn(paymentProof);

        PaymentProofVo paymentProofVo = paymentProofService.getLatestProof(100L);

        assertEquals(100L, paymentProofVo.invoiceId());
        assertEquals("tx-paid", paymentProofVo.txHash());
        assertEquals("ref_paid", paymentProofVo.referenceKey());
        assertEquals(InvoiceStatusEnum.PAID, paymentProofVo.finalStatus());
        assertEquals(List.of(ExceptionTagEnum.LATE_PAYMENT), paymentProofVo.exceptionTags());
    }

    @Test
    void shouldThrowWhenPaymentProofMissing() {
        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceMapper.selectById(100L)).thenReturn(invoice());
        when(paymentProofMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> paymentProofService.getLatestProof(100L));

        assertEquals(ErrorCode.PAYMENT_PROOF_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldSkipPersistingWhenPaymentProofAlreadyExists() {
        when(paymentProofMapper.selectCount(any())).thenReturn(1L);

        boolean saved = paymentProofService.saveIfAbsent(
            invoice(),
            paymentTransaction(),
            reconciliationRecord(),
            InvoiceStatusEnum.PAID,
            null,
            utc("2026-03-17T10:00:00Z")
        );

        assertFalse(saved);
    }

    private Invoice invoice() {
        Invoice invoice = new Invoice();
        invoice.setId(100L);
        invoice.setMerchantId(10L);
        invoice.setPublicId("pub_paid");
        invoice.setInvoiceNo("INV-001");
        return invoice;
    }

    private PaymentTransaction paymentTransaction() {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setId(1L);
        paymentTransaction.setInvoiceId(100L);
        paymentTransaction.setTxHash("tx-paid");
        paymentTransaction.setReferenceKey("ref_paid");
        paymentTransaction.setPayerAddress("payer-1");
        paymentTransaction.setRecipientAddress("recipient-1");
        paymentTransaction.setMintAddress("mint-1");
        paymentTransaction.setAmount(new BigDecimal("99.00"));
        paymentTransaction.setVerificationResult(PaymentVerificationResultEnum.PAID);
        return paymentTransaction;
    }

    private ReconciliationRecord reconciliationRecord() {
        ReconciliationRecord reconciliationRecord = new ReconciliationRecord();
        reconciliationRecord.setInvoiceId(100L);
        reconciliationRecord.setTxHash("tx-paid");
        reconciliationRecord.setReconciliationStatus(ReconciliationStatusEnum.SUCCESS);
        reconciliationRecord.setResultMessage("Invoice marked as paid.");
        return reconciliationRecord;
    }

    private OffsetDateTime utc(String value) {
        return OffsetDateTime.parse(value);
    }

    private record TestPaymentProofPayload(
        Long invoiceId,
        String publicId,
        String invoiceNo,
        String txHash,
        String referenceKey,
        String payerAddress,
        String recipientAddress,
        String mintAddress,
        BigDecimal amount,
        OffsetDateTime paidAt,
        PaymentVerificationResultEnum verificationResult,
        InvoiceStatusEnum finalStatus,
        List<String> exceptionTags,
        ReconciliationStatusEnum reconciliationStatus,
        String resultMessage
    ) {
    }
}
