package com.stableflow.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stableflow.security")
public record SecurityProperties(String jwtSecret) {
}
