package com.stableflow.reconciliation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.enums.ReconciliationStatusEnum;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional single-reconciliation service / 负责单笔已验证交易事务核销的服务 */
@Service
public class SingleReconciliationServiceImpl implements SingleReconciliationService {

    private final InvoiceService invoiceService;
    private final ReconciliationRecordService reconciliationRecordService;
    private final PaymentProofService paymentProofService;
    private final ObjectMapper objectMapper;

    public SingleReconciliationServiceImpl(
        InvoiceService invoiceService,
        ReconciliationRecordService reconciliationRecordService,
        PaymentProofService paymentProofService,
        ObjectMapper objectMapper
    ) {
        this.invoiceService = invoiceService;
        this.reconciliationRecordService = reconciliationRecordService;
        this.paymentProofService = paymentProofService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public boolean reconcileTransaction(PaymentTransaction paymentTransaction) {
        if (paymentTransaction == null || paymentTransaction.getInvoiceId() == null) {
            return false;
        }
        if (reconciliationRecordService.existsByInvoiceIdAndTxHash(paymentTransaction.getInvoiceId(), paymentTransaction.getTxHash())) {
            return false;
        }

        Invoice invoice = invoiceService.getById(paymentTransaction.getInvoiceId());
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND, "Invoice not found for reconciliation");
        }

        ReconciliationDecision decision = decideReconciliation(paymentTransaction, invoice);
        InvoiceSnapshot invoiceSnapshot = buildInvoiceSnapshot(invoice, paymentTransaction, decision);
        applyInvoiceUpdate(invoice.getId(), invoiceSnapshot);
        ReconciliationRecord reconciliationRecord = toReconciliationRecord(paymentTransaction, decision);
        reconciliationRecordService.saveIfAbsent(reconciliationRecord);
        paymentProofService.saveIfAbsent(
            invoice,
            paymentTransaction,
            reconciliationRecord,
            invoiceSnapshot.status(),
            invoiceSnapshot.exceptionTags(),
            invoiceSnapshot.paidAt()
        );
        return true;
    }

    private ReconciliationDecision decideReconciliation(PaymentTransaction paymentTransaction, Invoice invoice) {
        PaymentVerificationResultEnum verificationResult = paymentTransaction.getVerificationResult();
        if (verificationResult == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Verification result is required before reconciliation");
        }

        return switch (verificationResult) {
            case PAID -> new ReconciliationDecision(
                ReconciliationStatusEnum.SUCCESS,
                InvoiceStatusEnum.PAID,
                List.of(),
                "Invoice marked as paid."
            );
            case PARTIALLY_PAID -> new ReconciliationDecision(
                ReconciliationStatusEnum.SUCCESS,
                InvoiceStatusEnum.PARTIALLY_PAID,
                List.of(),
                "Invoice marked as partially paid."
            );
            case OVERPAID -> new ReconciliationDecision(
                ReconciliationStatusEnum.SUCCESS,
                InvoiceStatusEnum.OVERPAID,
                List.of(),
                "Invoice marked as overpaid."
            );
            case LATE_PAYMENT -> new ReconciliationDecision(
                ReconciliationStatusEnum.SUCCESS,
                InvoiceStatusEnum.EXPIRED,
                List.of(PaymentVerificationResultEnum.LATE_PAYMENT.getCode()),
                "Late payment recorded on an expired invoice."
            );
            case WRONG_CURRENCY -> new ReconciliationDecision(
                ReconciliationStatusEnum.SKIPPED,
                invoice.getStatus(),
                List.of(PaymentVerificationResultEnum.WRONG_CURRENCY.getCode()),
                "Skipped invoice update because the payment currency is wrong."
            );
            case DUPLICATE_PAYMENT -> new ReconciliationDecision(
                ReconciliationStatusEnum.SKIPPED,
                invoice.getStatus(),
                List.of(PaymentVerificationResultEnum.DUPLICATE_PAYMENT.getCode()),
                "Skipped invoice update because an earlier effective payment already exists."
            );
            case INVALID_REFERENCE, MISSING_REFERENCE -> new ReconciliationDecision(
                ReconciliationStatusEnum.SKIPPED,
                invoice.getStatus(),
                List.of(verificationResult.getCode()),
                "Skipped invoice update because the payment reference is not reconcilable."
            );
            case PENDING -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "Pending transactions cannot be reconciled");
        };
    }

    private InvoiceSnapshot buildInvoiceSnapshot(
        Invoice invoice,
        PaymentTransaction paymentTransaction,
        ReconciliationDecision decision
    ) {
        return new InvoiceSnapshot(
            decision.invoiceStatus(),
            mergeExceptionTags(invoice.getExceptionTags(), decision.exceptionTags()),
            resolvePaidAt(invoice.getPaidAt(), paymentTransaction.getBlockTime(), decision.invoiceStatus())
        );
    }

    private void applyInvoiceUpdate(Long invoiceId, InvoiceSnapshot invoiceSnapshot) {
        Invoice update = new Invoice();
        update.setId(invoiceId);
        update.setStatus(invoiceSnapshot.status());
        update.setExceptionTags(invoiceSnapshot.exceptionTags());
        update.setPaidAt(invoiceSnapshot.paidAt());
        invoiceService.updateById(update);
    }

    private OffsetDateTime resolvePaidAt(
        OffsetDateTime currentPaidAt,
        OffsetDateTime transactionBlockTime,
        InvoiceStatusEnum invoiceStatus
    ) {
        if (invoiceStatus == InvoiceStatusEnum.PAID
            || invoiceStatus == InvoiceStatusEnum.PARTIALLY_PAID
            || invoiceStatus == InvoiceStatusEnum.OVERPAID) {
            if (currentPaidAt == null) {
                return transactionBlockTime;
            }
            if (transactionBlockTime == null || currentPaidAt.isBefore(transactionBlockTime)) {
                return currentPaidAt;
            }
            return transactionBlockTime;
        }
        return currentPaidAt;
    }

    private ReconciliationRecord toReconciliationRecord(
        PaymentTransaction paymentTransaction,
        ReconciliationDecision decision
    ) {
        ReconciliationRecord reconciliationRecord = new ReconciliationRecord();
        reconciliationRecord.setInvoiceId(paymentTransaction.getInvoiceId());
        reconciliationRecord.setTxHash(paymentTransaction.getTxHash());
        reconciliationRecord.setReconciliationStatus(decision.reconciliationStatus());
        reconciliationRecord.setResultMessage(decision.message());
        reconciliationRecord.setExceptionTags(toJsonArray(decision.exceptionTags()));
        reconciliationRecord.setProcessedAt(OffsetDateTime.now());
        return reconciliationRecord;
    }

    private JsonNode toJsonArray(List<String> exceptionTags) {
        if (exceptionTags == null || exceptionTags.isEmpty()) {
            return null;
        }
        return objectMapper.valueToTree(exceptionTags);
    }

    private String mergeExceptionTags(String existingTags, List<String> newTags) {
        Set<String> mergedTags = new LinkedHashSet<>();
        if (existingTags != null && !existingTags.isBlank()) {
            for (String tag : existingTags.split(",")) {
                if (!tag.isBlank()) {
                    mergedTags.add(tag.trim());
                }
            }
        }
        if (newTags != null) {
            mergedTags.addAll(newTags);
        }
        return mergedTags.isEmpty() ? null : String.join(",", mergedTags);
    }

    private record ReconciliationDecision(
        /** Reconciliation record status / 核销记录状态 */
        ReconciliationStatusEnum reconciliationStatus,
        /** Target invoice status after reconciliation / 核销后的目标账单状态 */
        InvoiceStatusEnum invoiceStatus,
        /** Exception tags appended during reconciliation / 核销过程中追加的异常标签 */
        List<String> exceptionTags,
        /** Human-readable reconciliation message / 可读核销说明 */
        String message
    ) {
    }

    private record InvoiceSnapshot(
        /** Final invoice status after reconciliation / 核销后的最终账单状态 */
        InvoiceStatusEnum status,
        /** Final serialized exception tags / 最终序列化异常标签 */
        String exceptionTags,
        /** Final paid time in UTC / 最终支付时间（UTC） */
        OffsetDateTime paidAt
    ) {
    }
}
