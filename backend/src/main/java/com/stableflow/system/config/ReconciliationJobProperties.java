package com.stableflow.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Reconciliation job scheduling properties / 核销任务调度配置 */
@ConfigurationProperties(prefix = "stableflow.reconciliation.job")
public record ReconciliationJobProperties(
    /** Whether the reconciliation job is enabled / 是否启用核销任务 */
    boolean enabled,
    /** Maximum number of verified transactions reconciled per run / 单次任务最多处理的已验证交易数 */
    Integer batchSize,
    /** Fixed delay between reconciliation runs in milliseconds / 两次核销任务之间的固定间隔（毫秒） */
    Long fixedDelayMs,
    /** Initial delay before the first reconciliation run in milliseconds / 首次启动核销任务前的延迟时间（毫秒） */
    Long initialDelayMs
) {
}
