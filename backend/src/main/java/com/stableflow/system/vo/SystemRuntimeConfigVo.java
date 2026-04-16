package com.stableflow.system.vo;

import com.stableflow.system.enums.SolanaNetworkEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SystemRuntimeConfigVo", description = "Runtime configuration exposed to frontend / 暴露给前端的运行时配置")
public record SystemRuntimeConfigVo(
    @Schema(description = SolanaNetworkEnum.DESC, implementation = SolanaNetworkEnum.class)
    SolanaNetworkEnum solanaNetwork,
    @Schema(description = "Default mint address for the active Solana network / 当前 Solana 网络默认 Mint 地址")
    String defaultMintAddress,
    @Schema(description = "Base URL used to open Solana Explorer transaction details / 用于打开 Solana Explorer 交易详情的基础地址")
    String explorerTxBaseUrl
) {
}
