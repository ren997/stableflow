package com.stableflow.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stableflow.payment")
public record PaymentProperties(String orderAssociationMode, String publicBaseUrl) {
}
