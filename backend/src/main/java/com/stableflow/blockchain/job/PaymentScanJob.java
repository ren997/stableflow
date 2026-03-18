package com.stableflow.blockchain.job;

import com.stableflow.blockchain.service.PaymentScanService;
import com.stableflow.system.config.SolanaScanProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled job that scans active Solana recipient addresses for new candidate payments / 定时扫描启用中的 Solana 收款地址，发现新的候选支付交易 */
@Component
@RequiredArgsConstructor
public class PaymentScanJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentScanJob.class);

    private final PaymentScanService paymentScanService;
    private final SolanaScanProperties solanaScanProperties;

    @Scheduled(
        fixedDelayString = "${stableflow.solana.scan.fixed-delay-ms:30000}",
        initialDelayString = "${stableflow.solana.scan.initial-delay-ms:10000}"
    )
    /** Run incremental payment scanning on a fixed delay when scan scheduling is enabled / 在启用扫描调度时按固定间隔执行增量支付扫描 */
    public void scanPayments() {
        if (!solanaScanProperties.enabled()) {
            return;
        }

        int insertedCount = paymentScanService.scanAllActiveAddresses();
        log.info("PaymentScanJob finished, insertedCount={}", insertedCount);
    }
}
