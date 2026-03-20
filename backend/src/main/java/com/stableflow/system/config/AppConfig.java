package com.stableflow.system.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    SecurityProperties.class,
    PaymentProperties.class,
    SolanaProperties.class,
    SolanaScanProperties.class,
    InvoiceExpireJobProperties.class,
    PaymentVerifyProperties.class,
    ReconciliationJobProperties.class
})
public class AppConfig {
}
