package com.stableflow.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "MerchantPaymentConfigRequestDto", description = "Merchant payment config request / 商家收款配置请求")
public record MerchantPaymentConfigRequestDto(
    @Schema(description = "Merchant fixed wallet address / 商家固定收款地址", example = "7xKXtg2CW5ywQ2RkW9sQn8dM8pQ6eG3fQY1Qe6mVnW7K")
    @NotBlank String walletAddress,
    @Schema(description = "USDC mint address / USDC Mint 地址", example = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")
    @NotBlank String mintAddress,
    @Schema(description = "Blockchain name / 链名称", example = "SOLANA")
    @NotBlank String chain
) {
}
