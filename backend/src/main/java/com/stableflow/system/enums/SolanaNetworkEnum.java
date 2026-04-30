package com.stableflow.system.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Solana network enum used by runtime configuration and blockchain integration / 供运行时配置与链上集成使用的 Solana 网络枚举 */
@Getter
public enum SolanaNetworkEnum {
    /** Solana devnet environment / Solana Devnet 环境 */
    DEVNET(
        "DEVNET",
        "开发测试网",
        "https://api.devnet.solana.com",
        "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
        "https://explorer.solana.com/tx/"
    ),
    /** Solana mainnet-beta environment / Solana Mainnet 环境 */
    MAINNET(
        "MAINNET",
        "主网",
        "https://api.mainnet-beta.solana.com",
        "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
        "https://explorer.solana.com/tx/"
    );

    public static final String DESC = "Solana network: DEVNET-开发测试网, MAINNET-主网";

    private static final Map<String, SolanaNetworkEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(SolanaNetworkEnum::getCode, Function.identity()));

    /** Stable code exposed to config and API payloads / 暴露给配置与接口载荷的稳定编码 */
    @JsonValue
    private final String code;

    /** Human-readable network description / 可读网络说明 */
    private final String desc;

    /** Default RPC URL for the network / 网络默认 RPC 地址 */
    private final String defaultRpcUrl;

    /** Default USDC mint address for the network / 网络默认 USDC Mint 地址 */
    private final String defaultUsdcMintAddress;

    /** Base URL used to open transaction details in Solana Explorer / 用于打开 Solana Explorer 交易详情的基础地址 */
    private final String explorerTxBaseUrl;

    SolanaNetworkEnum(
        String code,
        String desc,
        String defaultRpcUrl,
        String defaultUsdcMintAddress,
        String explorerTxBaseUrl
    ) {
        this.code = code;
        this.desc = desc;
        this.defaultRpcUrl = defaultRpcUrl;
        this.defaultUsdcMintAddress = defaultUsdcMintAddress;
        this.explorerTxBaseUrl = explorerTxBaseUrl;
    }

    /** Resolve enum by code / 按编码解析枚举 */
    public static SolanaNetworkEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
