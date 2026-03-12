package com.stableflow.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stableflow.solana")
public record SolanaProperties(String rpcUrl, String usdcMintAddress) {
}
