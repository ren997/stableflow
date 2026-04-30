package com.stableflow.system.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stableflow.system.enums.SolanaNetworkEnum;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SolanaPropertiesTest {

    @Test
    void shouldResolveDevnetDefaultsWhenOverridesAreMissing() {
        SolanaProperties properties = new SolanaProperties(
            SolanaNetworkEnum.DEVNET,
            null,
            null,
            Duration.ofSeconds(3),
            Duration.ofSeconds(10),
            3,
            Duration.ofMillis(500)
        );

        assertEquals(SolanaNetworkEnum.DEVNET, properties.resolvedNetwork());
        assertEquals("https://api.devnet.solana.com", properties.resolvedRpcUrl());
        assertEquals("4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU", properties.resolvedUsdcMintAddress());
        assertEquals("https://explorer.solana.com/tx/", properties.resolvedExplorerTxBaseUrl());
    }

    @Test
    void shouldResolveMainnetDefaultsWhenNetworkSwitches() {
        SolanaProperties properties = new SolanaProperties(
            SolanaNetworkEnum.MAINNET,
            null,
            null,
            Duration.ofSeconds(3),
            Duration.ofSeconds(10),
            3,
            Duration.ofMillis(500)
        );

        assertEquals(SolanaNetworkEnum.MAINNET, properties.resolvedNetwork());
        assertEquals("https://api.mainnet-beta.solana.com", properties.resolvedRpcUrl());
        assertEquals("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", properties.resolvedUsdcMintAddress());
    }

    @Test
    void shouldPreferExplicitOverridesOverNetworkDefaults() {
        SolanaProperties properties = new SolanaProperties(
            SolanaNetworkEnum.DEVNET,
            "https://rpc.example.com",
            "custom-mint",
            Duration.ofSeconds(3),
            Duration.ofSeconds(10),
            3,
            Duration.ofMillis(500)
        );

        assertEquals("https://rpc.example.com", properties.resolvedRpcUrl());
        assertEquals("custom-mint", properties.resolvedUsdcMintAddress());
    }
}
