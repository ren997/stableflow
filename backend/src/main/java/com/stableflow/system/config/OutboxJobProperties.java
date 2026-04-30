package com.stableflow.system.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Outbox dispatch job scheduling properties / outbox 分发任务调度配置 */
@ConfigurationProperties(prefix = "stableflow.outbox.job")
public record OutboxJobProperties(
    /** Whether the outbox dispatch job is enabled / 是否启用 outbox 分发任务 */
    boolean enabled,
    /** Maximum number of outbox events dispatched per run / 单次任务最多处理的 outbox 事件数 */
    Integer batchSize,
    /** Fixed delay between dispatch runs in milliseconds / 两次分发任务之间的固定间隔（毫秒） */
    Long fixedDelayMs,
    /** Initial delay before the first dispatch run in milliseconds / 首次启动分发任务前的延迟时间（毫秒） */
    Long initialDelayMs,
    /** Whether to guard the dispatch job with a distributed lock / 是否使用分布式锁保护分发任务 */
    boolean lockEnabled,
    /** Distributed lock key / 分布式锁键 */
    String lockKey,
    /** Distributed lock TTL / 分布式锁过期时间 */
    Duration lockTtl,
    /** Retry delay after a failed dispatch / 分发失败后的重试延迟 */
    Duration retryDelay
) {
}
