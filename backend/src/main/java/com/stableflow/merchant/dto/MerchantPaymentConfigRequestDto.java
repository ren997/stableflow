package com.stableflow.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "MerchantPaymentConfigRequestDto", description = "Merchant payment config request / 商家收款配置请求")
public record MerchantPaymentConfigRequestDto(
    @Schema(description = "Merchant fixed wallet address / 商家固定收款地址", example = "7xKXtg2CW5ywQ2RkW9sQn8dM8pQ6eG3fQY1Qe6mVnW7K")
    @NotBlank @Size(max = 128) String walletAddress
) {
}
