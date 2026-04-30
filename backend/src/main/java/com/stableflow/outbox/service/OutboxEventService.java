package com.stableflow.outbox.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.outbox.entity.OutboxEvent;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import java.time.OffsetDateTime;
import java.util.List;

public interface OutboxEventService extends IService<OutboxEvent> {

    /** Persist an invoice payment result event in the same transaction as reconciliation / 在与核销相同的事务里保存账单支付结果事件 */
    boolean saveInvoicePaymentResultEvent(
        Invoice invoice,
        PaymentTransaction paymentTransaction,
        ReconciliationRecord reconciliationRecord,
        InvoiceStatusEnum finalStatus,
        List<String> exceptionTags,
        OffsetDateTime paidAt
    );

    /** Load dispatchable outbox events that are ready now / 加载当前可分发的 outbox 事件 */
    List<OutboxEvent> listDispatchableEvents(int limit, OffsetDateTime now);

    /** Try to claim an outbox event before dispatch / 在分发前尝试认领 outbox 事件 */
    boolean markDispatching(Long outboxEventId, OffsetDateTime dispatchStartedAt);

    /** Mark an outbox event as dispatched / 标记 outbox 事件已分发 */
    void markDispatched(Long outboxEventId, OffsetDateTime dispatchedAt);

    /** Mark an outbox event as failed and schedule its next retry / 标记 outbox 事件分发失败并安排下次重试 */
    void markFailed(Long outboxEventId, String lastError, OffsetDateTime failedAt, OffsetDateTime nextRetryAt);
}
