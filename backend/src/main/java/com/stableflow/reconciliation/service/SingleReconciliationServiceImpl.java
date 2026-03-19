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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional single-reconciliation service / 负责单笔已验证交易事务核销的服务 */
@Service
@RequiredArgsConstructor
public class SingleReconciliationServiceImpl implements SingleReconciliationService {

    private final InvoiceService invoiceService;
    private final ReconciliationRecordService reconciliationRecordService;
    private final PaymentProofService paymentProofService;
    private final ObjectMapper objectMapper;

    /** Reconcile one verified transaction into invoice state, reconciliation record, and payment proof / 将一笔已验证交易核销到账单状态、核销记录与支付凭证 */
    @Transactional
    @Override
    public boolean reconcileTransaction(PaymentTransaction paymentTransaction) {
        // 第一步先做幂等前置检查，缺少 invoice 归属或已处理过的交易直接跳过。
        if (paymentTransaction == null || paymentTransaction.getInvoiceId() == null) {
            return false;
        }
        if (reconciliationRecordService.existsByInvoiceIdAndTxHash(paymentTransaction.getInvoiceId(), paymentTransaction.getTxHash())) {
            return false;
        }

        // 第二步加载目标账单，后续状态流转和异常标签都基于当前账单快照计算。
        Invoice invoice = invoiceService.getById(paymentTransaction.getInvoiceId());
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_FOUND, "Invoice not found for reconciliation");
        }

        // 第三步根据验证结果生成核销决策，明确账单状态、异常标签和核销说明。
        ReconciliationDecision decision = decideReconciliation(paymentTransaction, invoice);

        // 第四步计算账单更新快照，并先落库账单状态，确保后续凭证读取到的是最新业务结果。
        InvoiceSnapshot invoiceSnapshot = buildInvoiceSnapshot(invoice, paymentTransaction, decision);
        applyInvoiceUpdate(invoice.getId(), invoiceSnapshot);

        // 第五步沉淀核销记录，保留这笔交易为什么被认账或跳过的处理痕迹。
        ReconciliationRecord reconciliationRecord = toReconciliationRecord(paymentTransaction, decision);
        reconciliationRecordService.saveIfAbsent(reconciliationRecord);

        // 第六步生成支付凭证快照，对外提供可查询的支付结果证据视图。
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
        // 核销层只消费已完成验证的交易，不接受仍处于待验证状态的数据。
        PaymentVerificationResultEnum verificationResult = paymentTransaction.getVerificationResult();
        if (verificationResult == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Verification result is required before reconciliation");
        }

        // 把验证结论映射为核销动作，决定是否更新账单、是否补异常标签，以及最终说明文案。
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
        // 把目标状态、异常标签和 paidAt 一次性算清楚，避免分散更新时遗漏字段。
        return new InvoiceSnapshot(
            decision.invoiceStatus(),
            mergeExceptionTags(invoice.getExceptionTags(), decision.exceptionTags()),
            resolvePaidAt(invoice.getPaidAt(), paymentTransaction.getBlockTime(), decision.invoiceStatus())
        );
    }

    private void applyInvoiceUpdate(Long invoiceId, InvoiceSnapshot invoiceSnapshot) {
        // 只更新核销需要变动的字段，避免覆盖账单上的无关信息。
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
        // paidAt 仅在有效支付结果下更新，并保留更早的有效支付时间作为最终支付时间。
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
        // 核销记录保存的是处理结论，而不是完整账单快照，方便后续审计和排障。
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

    private List<String> mergeExceptionTags(List<String> existingTags, List<String> newTags) {
        // 保持异常标签去重且追加式合并，避免后一次核销把已有异常信息冲掉。
        Set<String> mergedTags = new LinkedHashSet<>();
        if (existingTags != null) {
            existingTags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .forEach(mergedTags::add);
        }
        if (newTags != null) {
            newTags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .forEach(mergedTags::add);
        }
        return mergedTags.isEmpty() ? null : List.copyOf(mergedTags);
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
        /** Final exception tag code list / 最终异常标签编码列表 */
        List<String> exceptionTags,
        /** Final paid time in UTC / 最终支付时间（UTC） */
        OffsetDateTime paidAt
    ) {
    }
}
