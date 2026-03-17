package com.stableflow.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Payment verification job scheduling properties / 支付验证任务调度配置 */
@ConfigurationProperties(prefix = "stableflow.verification.job")
public record PaymentVerifyProperties(
    /** Whether the payment verification job is enabled / 是否启用支付验证任务 */
    boolean enabled,
    /** Maximum number of pending transactions verified per run / 单次任务最多处理的待验证交易数 */
    Integer batchSize,
    /** Fixed delay between verification runs in milliseconds / 两次验证任务之间的固定间隔（毫秒） */
    Long fixedDelayMs,
    /** Initial delay before the first verification run in milliseconds / 首次启动验证任务前的延迟时间（毫秒） */
    Long initialDelayMs
) {
}
