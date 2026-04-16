package com.stableflow.system.config;

import com.stableflow.system.enums.SolanaNetworkEnum;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stableflow.solana")
public record SolanaProperties(
    SolanaNetworkEnum network,
    String rpcUrl,
    String usdcMintAddress,
    Duration connectTimeout,
    Duration readTimeout,
    Integer retryMaxAttempts,
    Duration retryDelay
) {

    public SolanaNetworkEnum resolvedNetwork() {
        return network == null ? SolanaNetworkEnum.DEVNET : network;
    }

    public String resolvedRpcUrl() {
        return hasText(rpcUrl) ? rpcUrl : resolvedNetwork().getDefaultRpcUrl();
    }

    public String resolvedUsdcMintAddress() {
        return hasText(usdcMintAddress) ? usdcMintAddress : resolvedNetwork().getDefaultUsdcMintAddress();
    }

    public String resolvedExplorerTxBaseUrl() {
        return resolvedNetwork().getExplorerTxBaseUrl();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
