package com.stableflow.blockchain.job;

import com.stableflow.blockchain.service.PaymentScanService;
import com.stableflow.system.config.SolanaScanProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentScanJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentScanJob.class);

    private final PaymentScanService paymentScanService;
    private final SolanaScanProperties solanaScanProperties;

    public PaymentScanJob(
        PaymentScanService paymentScanService,
        SolanaScanProperties solanaScanProperties
    ) {
        this.paymentScanService = paymentScanService;
        this.solanaScanProperties = solanaScanProperties;
    }

    @Scheduled(
        fixedDelayString = "${stableflow.solana.scan.fixed-delay-ms:30000}",
        initialDelayString = "${stableflow.solana.scan.initial-delay-ms:10000}"
    )
    public void scanPayments() {
        if (!solanaScanProperties.enabled()) {
            return;
        }

        int insertedCount = paymentScanService.scanAllActiveAddresses();
        log.info("PaymentScanJob finished, insertedCount={}", insertedCount);
    }
}
