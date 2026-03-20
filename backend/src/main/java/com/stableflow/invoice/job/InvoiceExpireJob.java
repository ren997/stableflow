package com.stableflow.invoice.job;

import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.system.config.InvoiceExpireJobProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled job that expires overdue pending invoices / 定时扫描并过期已超时的待支付账单 */
@Component
@RequiredArgsConstructor
public class InvoiceExpireJob {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExpireJob.class);

    private final InvoiceService invoiceService;
    private final InvoiceExpireJobProperties invoiceExpireJobProperties;

    @Scheduled(
        fixedDelayString = "${stableflow.invoice.expire-job.fixed-delay-ms:30000}",
        initialDelayString = "${stableflow.invoice.expire-job.initial-delay-ms:12000}"
    )
    /** Run overdue invoice expiration on a fixed delay when scheduling is enabled / 在启用任务调度时按固定间隔执行超期账单过期处理 */
    public void expirePendingInvoices() {
        if (!invoiceExpireJobProperties.enabled()) {
            return;
        }

        int expiredCount = invoiceService.expirePendingInvoices();
        log.info("InvoiceExpireJob finished, expiredCount={}", expiredCount);
    }
}
