package com.stableflow.reconciliation.job;

import com.stableflow.reconciliation.service.ReconciliationService;
import com.stableflow.system.config.ReconciliationJobProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled job that applies verified payment results to invoice reconciliation state / 定时把已验证支付结果应用到账单核销状态 */
@Component
@RequiredArgsConstructor
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final ReconciliationService reconciliationService;
    private final ReconciliationJobProperties reconciliationJobProperties;

    @Scheduled(
        fixedDelayString = "${stableflow.reconciliation.job.fixed-delay-ms:30000}",
        initialDelayString = "${stableflow.reconciliation.job.initial-delay-ms:20000}"
    )
    /** Run reconciliation on a fixed delay when reconciliation scheduling is enabled / 在启用核销调度时按固定间隔执行已验证交易核销 */
    public void reconcileVerifiedPayments() {
        if (!reconciliationJobProperties.enabled()) {
            return;
        }

        int batchSize = resolveBatchSize();
        int reconciledCount = reconciliationService.reconcilePendingTransactions(batchSize);
        log.info("ReconciliationJob finished, batchSize={}, reconciledCount={}", batchSize, reconciledCount);
    }

    private int resolveBatchSize() {
        return reconciliationJobProperties.batchSize() == null || reconciliationJobProperties.batchSize() <= 0
            ? 50
            : reconciliationJobProperties.batchSize();
    }
}
