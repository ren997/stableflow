package com.stableflow.outbox.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.outbox.entity.OutboxEvent;
import com.stableflow.outbox.service.OutboxEventService;
import com.stableflow.system.config.OutboxJobProperties;
import com.stableflow.system.lock.JobLockService;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled job that dispatches reliable outbox events for downstream consumers / 定时分发可靠 outbox 事件给后续消费者 */
@Component
@RequiredArgsConstructor
public class OutboxDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchJob.class);
    private static final String DEFAULT_LOCK_KEY = "stableflow:job:outbox-dispatch:lock";
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMinutes(1);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_ERROR_LENGTH = 512;

    private final OutboxEventService outboxEventService;
    private final OutboxJobProperties outboxJobProperties;
    private final JobLockService jobLockService;

    @Scheduled(
        fixedDelayString = "${stableflow.outbox.job.fixed-delay-ms:30000}",
        initialDelayString = "${stableflow.outbox.job.initial-delay-ms:25000}"
    )
    /** Run outbox dispatch on a fixed delay when scheduling is enabled / 在启用调度时按固定间隔执行 outbox 分发 */
    public void dispatchOutboxEvents() {
        if (!outboxJobProperties.enabled()) {
            return;
        }

        if (!outboxJobProperties.lockEnabled()) {
            runDispatch();
            return;
        }

        String lockKey = resolveLockKey();
        String lockValue = UUID.randomUUID().toString();
        if (!jobLockService.tryLock(lockKey, lockValue, resolveLockTtl())) {
            log.info("OutboxDispatchJob skipped because lock is already held, lockKey={}", lockKey);
            return;
        }

        try {
            runDispatch();
        } finally {
            jobLockService.unlock(lockKey, lockValue);
        }
    }

    private void runDispatch() {
        int dispatchedCount = 0;
        int failedCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (OutboxEvent outboxEvent : outboxEventService.listDispatchableEvents(resolveBatchSize(), now)) {
            if (!outboxEventService.markDispatching(outboxEvent.getId(), now)) {
                continue;
            }

            try {
                dispatchEvent(outboxEvent);
                outboxEventService.markDispatched(outboxEvent.getId(), OffsetDateTime.now());
                dispatchedCount++;
            } catch (RuntimeException ex) {
                outboxEventService.markFailed(
                    outboxEvent.getId(),
                    abbreviateError(ex),
                    OffsetDateTime.now(),
                    OffsetDateTime.now().plus(resolveRetryDelay())
                );
                failedCount++;
                log.error(
                    "Outbox dispatch failed, outboxEventId={}, eventType={}, aggregateType={}, aggregateId={}, merchantId={}, invoiceId={}, reference={}, txHash={}",
                    outboxEvent.getId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getAggregateType(),
                    outboxEvent.getAggregateId(),
                    payloadText(outboxEvent.getPayload(), "merchantId"),
                    payloadText(outboxEvent.getPayload(), "invoiceId"),
                    payloadText(outboxEvent.getPayload(), "referenceKey"),
                    payloadText(outboxEvent.getPayload(), "txHash"),
                    ex
                );
            }
        }

        log.info(
            "OutboxDispatchJob finished, batchSize={}, dispatchedCount={}, failedCount={}",
            resolveBatchSize(),
            dispatchedCount,
            failedCount
        );
    }

    private void dispatchEvent(OutboxEvent outboxEvent) {
        // 当前先把 outbox 分发收敛成可靠日志出口，后续可以在此平滑接入 Webhook、通知或 Agent 消费者。
        log.info(
            "Outbox event dispatched, outboxEventId={}, eventType={}, aggregateType={}, aggregateId={}, merchantId={}, invoiceId={}, reference={}, txHash={}, finalStatus={}",
            outboxEvent.getId(),
            outboxEvent.getEventType(),
            outboxEvent.getAggregateType(),
            outboxEvent.getAggregateId(),
            payloadText(outboxEvent.getPayload(), "merchantId"),
            payloadText(outboxEvent.getPayload(), "invoiceId"),
            payloadText(outboxEvent.getPayload(), "referenceKey"),
            payloadText(outboxEvent.getPayload(), "txHash"),
            payloadText(outboxEvent.getPayload(), "finalStatus")
        );
    }

    private int resolveBatchSize() {
        return outboxJobProperties.batchSize() == null || outboxJobProperties.batchSize() <= 0
            ? DEFAULT_BATCH_SIZE
            : outboxJobProperties.batchSize();
    }

    private String resolveLockKey() {
        return outboxJobProperties.lockKey() == null || outboxJobProperties.lockKey().isBlank()
            ? DEFAULT_LOCK_KEY
            : outboxJobProperties.lockKey();
    }

    private Duration resolveLockTtl() {
        return outboxJobProperties.lockTtl() == null || outboxJobProperties.lockTtl().isNegative()
            ? DEFAULT_LOCK_TTL
            : outboxJobProperties.lockTtl();
    }

    private Duration resolveRetryDelay() {
        return outboxJobProperties.retryDelay() == null || outboxJobProperties.retryDelay().isNegative()
            ? DEFAULT_RETRY_DELAY
            : outboxJobProperties.retryDelay();
    }

    private String payloadText(JsonNode payload, String fieldName) {
        if (payload == null) {
            return null;
        }
        JsonNode valueNode = payload.get(fieldName);
        return valueNode == null || valueNode.isNull() ? null : valueNode.asText();
    }

    private String abbreviateError(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
