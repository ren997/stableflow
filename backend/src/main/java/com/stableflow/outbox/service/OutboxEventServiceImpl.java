package com.stableflow.outbox.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.outbox.entity.OutboxEvent;
import com.stableflow.outbox.enums.OutboxAggregateTypeEnum;
import com.stableflow.outbox.enums.OutboxEventStatusEnum;
import com.stableflow.outbox.enums.OutboxEventTypeEnum;
import com.stableflow.outbox.mapper.OutboxEventMapper;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.enums.ReconciliationStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service that manages reliable outbox persistence and dispatch status transitions / 管理可靠 outbox 持久化与分发状态流转的服务 */
@Service
public class OutboxEventServiceImpl extends ServiceImpl<OutboxEventMapper, OutboxEvent> implements OutboxEventService {

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    public OutboxEventServiceImpl(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Transactional
    @Override
    public boolean saveInvoicePaymentResultEvent(
        Invoice invoice,
        PaymentTransaction paymentTransaction,
        ReconciliationRecord reconciliationRecord,
        InvoiceStatusEnum finalStatus,
        List<String> exceptionTags,
        OffsetDateTime paidAt
    ) {
        if (invoice == null || invoice.getId() == null || paymentTransaction == null || paymentTransaction.getTxHash() == null) {
            return false;
        }

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType(OutboxEventTypeEnum.INVOICE_PAYMENT_RESULT);
        outboxEvent.setAggregateType(OutboxAggregateTypeEnum.INVOICE);
        outboxEvent.setAggregateId(String.valueOf(invoice.getId()));
        outboxEvent.setPayload(
            objectMapper.valueToTree(
                new InvoicePaymentResultPayload(
                    invoice.getId(),
                    invoice.getPublicId(),
                    invoice.getInvoiceNo(),
                    invoice.getMerchantId(),
                    paymentTransaction.getId(),
                    paymentTransaction.getTxHash(),
                    paymentTransaction.getReferenceKey(),
                    paymentTransaction.getPayerAddress(),
                    paymentTransaction.getRecipientAddress(),
                    paymentTransaction.getMintAddress(),
                    paymentTransaction.getAmount(),
                    paymentTransaction.getCurrency(),
                    paymentTransaction.getBlockTime(),
                    paymentTransaction.getVerificationResult(),
                    reconciliationRecord == null ? null : reconciliationRecord.getReconciliationStatus(),
                    finalStatus,
                    exceptionTags == null ? List.of() : List.copyOf(exceptionTags),
                    paidAt,
                    OffsetDateTime.now()
                )
            )
        );
        outboxEvent.setStatus(OutboxEventStatusEnum.PENDING);
        outboxEvent.setRetryCount(0);
        outboxEvent.setLastError(null);
        outboxEvent.setNextRetryAt(null);
        return outboxEventMapper.insert(outboxEvent) > 0;
    }

    @Override
    public List<OutboxEvent> listDispatchableEvents(int limit, OffsetDateTime now) {
        int safeLimit = limit <= 0 ? 50 : limit;
        return outboxEventMapper.selectList(
            new LambdaQueryWrapper<OutboxEvent>()
                .and(
                    wrapper -> wrapper
                        .eq(OutboxEvent::getStatus, OutboxEventStatusEnum.PENDING)
                        .or()
                        .eq(OutboxEvent::getStatus, OutboxEventStatusEnum.FAILED)
                )
                .and(
                    wrapper -> wrapper
                        .isNull(OutboxEvent::getNextRetryAt)
                        .or()
                        .le(OutboxEvent::getNextRetryAt, now)
                )
                .orderByAsc(OutboxEvent::getCreatedAt)
                .orderByAsc(OutboxEvent::getId)
                .last("LIMIT " + safeLimit)
        );
    }

    @Override
    public boolean markDispatching(Long outboxEventId, OffsetDateTime dispatchStartedAt) {
        if (outboxEventId == null) {
            return false;
        }
        return outboxEventMapper.update(
            null,
            new LambdaUpdateWrapper<OutboxEvent>()
                .eq(OutboxEvent::getId, outboxEventId)
                .and(
                    wrapper -> wrapper
                        .eq(OutboxEvent::getStatus, OutboxEventStatusEnum.PENDING)
                        .or()
                        .eq(OutboxEvent::getStatus, OutboxEventStatusEnum.FAILED)
                )
                .set(OutboxEvent::getStatus, OutboxEventStatusEnum.DISPATCHING)
                .set(OutboxEvent::getUpdatedAt, dispatchStartedAt)
        ) > 0;
    }

    @Override
    public void markDispatched(Long outboxEventId, OffsetDateTime dispatchedAt) {
        if (outboxEventId == null) {
            return;
        }
        outboxEventMapper.update(
            null,
            new LambdaUpdateWrapper<OutboxEvent>()
                .eq(OutboxEvent::getId, outboxEventId)
                .eq(OutboxEvent::getStatus, OutboxEventStatusEnum.DISPATCHING)
                .set(OutboxEvent::getStatus, OutboxEventStatusEnum.DISPATCHED)
                .set(OutboxEvent::getLastError, null)
                .set(OutboxEvent::getNextRetryAt, null)
                .set(OutboxEvent::getUpdatedAt, dispatchedAt)
        );
    }

    @Override
    public void markFailed(Long outboxEventId, String lastError, OffsetDateTime failedAt, OffsetDateTime nextRetryAt) {
        if (outboxEventId == null) {
            return;
        }
        outboxEventMapper.update(
            null,
            new UpdateWrapper<OutboxEvent>()
                .eq("id", outboxEventId)
                .eq("status", OutboxEventStatusEnum.DISPATCHING.getCode())
                .set("status", OutboxEventStatusEnum.FAILED.getCode())
                .set("last_error", lastError)
                .set("next_retry_at", nextRetryAt)
                .set("updated_at", failedAt)
                .setSql("retry_count = retry_count + 1")
        );
    }

    private record InvoicePaymentResultPayload(
        /** Invoice id / 账单 ID */
        Long invoiceId,
        /** Public invoice id / 公开账单标识 */
        String publicId,
        /** Invoice number / 账单编号 */
        String invoiceNo,
        /** Merchant id / 商家 ID */
        Long merchantId,
        /** Payment transaction id / 候选支付交易 ID */
        Long paymentTransactionId,
        /** Blockchain transaction hash / 链上交易哈希 */
        String txHash,
        /** Invoice reference key / 账单 reference */
        String referenceKey,
        /** Payer wallet address / 付款地址 */
        String payerAddress,
        /** Recipient wallet address / 收款地址 */
        String recipientAddress,
        /** Token mint address / 代币 Mint 地址 */
        String mintAddress,
        /** Paid amount / 支付金额 */
        BigDecimal amount,
        /** Currency code / 币种代码 */
        String currency,
        /** On-chain block time in UTC / 链上区块时间（UTC） */
        OffsetDateTime blockTime,
        /** Payment verification result / 支付验证结果 */
        PaymentVerificationResultEnum verificationResult,
        /** Reconciliation status / 核销状态 */
        ReconciliationStatusEnum reconciliationStatus,
        /** Final invoice status / 最终账单状态 */
        InvoiceStatusEnum finalStatus,
        /** Exception tags / 异常标签 */
        List<String> exceptionTags,
        /** Final paid time / 最终支付时间 */
        OffsetDateTime paidAt,
        /** Event emitted time / 事件发出时间 */
        OffsetDateTime emittedAt
    ) {
    }
}
