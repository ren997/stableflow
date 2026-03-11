package com.stableflow.merchant.dto;

import jakarta.validation.constraints.NotBlank;

public record MerchantPaymentConfigRequestDto(
    @NotBlank String walletAddress,
    @NotBlank String mintAddress,
    @NotBlank String chain
) {
}
