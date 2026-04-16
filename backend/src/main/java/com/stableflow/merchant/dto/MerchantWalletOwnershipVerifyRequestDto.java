package com.stableflow.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
    name = "MerchantWalletOwnershipVerifyRequestDto",
    description = "Wallet ownership signature submit request / 钱包地址所有权签名提交请求"
)
public record MerchantWalletOwnershipVerifyRequestDto(
    @Schema(description = "Challenge code returned by the challenge API / 挑战接口返回的挑战码")
    @NotBlank @Size(max = 128) String challengeCode,
    @Schema(description = "Wallet signature over the challenge message / 对挑战消息的链上钱包签名")
    @NotBlank @Size(max = 2048) String signature
) {
}
