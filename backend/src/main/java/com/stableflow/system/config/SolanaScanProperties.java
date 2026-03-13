package com.stableflow.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stableflow.solana.scan")
public record SolanaScanProperties(
    boolean enabled,
    Integer batchSize,
    Long fixedDelayMs,
    Long initialDelayMs
) {
}
