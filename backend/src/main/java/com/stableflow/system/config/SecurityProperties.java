package com.stableflow.system.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stableflow.security")
public record SecurityProperties(String jwtSecret, List<String> allowedOrigins, Duration tokenTtl) {
}
