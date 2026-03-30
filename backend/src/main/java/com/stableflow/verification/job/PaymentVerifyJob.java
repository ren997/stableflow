package com.stableflow.verification.job;

import com.stableflow.system.config.PaymentVerifyProperties;
import com.stableflow.verification.service.PaymentVerificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled job that verifies pending candidate transactions discovered by scanning / 定时验证扫描得到的待处理候选交易 */
@Component
@RequiredArgsConstructor
public class PaymentVerifyJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentVerifyJob.class);

    private final PaymentVerificationService paymentVerificationService;
    private final PaymentVerifyProperties paymentVerifyProperties;

    @Scheduled(
        fixedDelayString = "${stableflow.verification.job.fixed-delay-ms:30000}",
        initialDelayString = "${stableflow.verification.job.initial-delay-ms:15000}"
    )
    /** Run pending payment verification on a fixed delay when verification scheduling is enabled / 在启用验证调度时按固定间隔执行待验证交易处理 */
    public void verifyPendingPayments() {
        // 先检查任务开关，避免在未准备好的环境里自动消费待验证交易。
        if (!paymentVerifyProperties.enabled()) {
            return;
        }

        // 按配置批次处理 PENDING 候选交易，让扫描和验证两个阶段保持解耦。
        int batchSize = resolveBatchSize();
        int verifiedCount = paymentVerificationService.verifyPendingTransactions(batchSize);
        log.info("PaymentVerifyJob finished, batchSize={}, verifiedCount={}", batchSize, verifiedCount);
    }

    private int resolveBatchSize() {
        // 配置缺失或非法时回退默认批大小，避免任务因为配置问题完全失效。
        return paymentVerifyProperties.batchSize() == null || paymentVerifyProperties.batchSize() <= 0
            ? 50
            : paymentVerifyProperties.batchSize();
    }
}
