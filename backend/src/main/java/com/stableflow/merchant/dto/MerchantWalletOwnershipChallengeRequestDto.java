package com.stableflow.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Wallet ownership challenge request / 钱包地址所有权挑战请求 */
@Schema(
    name = "MerchantWalletOwnershipChallengeRequestDto",
    description = "Wallet ownership challenge request / 钱包地址所有权挑战请求"
)
public record MerchantWalletOwnershipChallengeRequestDto() {
}
