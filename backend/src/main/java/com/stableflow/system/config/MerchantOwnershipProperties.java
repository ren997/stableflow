package com.stableflow.system.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stableflow.merchant.ownership")
public record MerchantOwnershipProperties(Duration challengeTtl) {
}
