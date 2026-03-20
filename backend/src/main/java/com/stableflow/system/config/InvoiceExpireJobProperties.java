package com.stableflow.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Invoice expire job scheduling properties / 账单过期任务调度配置 */
@ConfigurationProperties(prefix = "stableflow.invoice.expire-job")
public record InvoiceExpireJobProperties(
    /** Whether the invoice expire job is enabled / 是否启用账单过期任务 */
    boolean enabled,
    /** Fixed delay between expire runs in milliseconds / 两次账单过期任务之间的固定间隔（毫秒） */
    Long fixedDelayMs,
    /** Initial delay before the first expire run in milliseconds / 首次启动账单过期任务前的延迟时间（毫秒） */
    Long initialDelayMs
) {
}
