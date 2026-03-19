package com.stableflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequestDto", description = "Merchant registration request / 商家注册请求")
public record RegisterRequestDto(
    @Schema(description = "Merchant display name / 商家名称", example = "StableFlow Demo")
    @NotBlank @Size(max = 128) String merchantName,
    @Schema(description = "Merchant email / 商家邮箱", example = "demo@stableflow.com")
    @NotBlank @Email @Size(max = 255) String email,
    @Schema(description = "Merchant password / 商家密码", example = "Password123")
    @NotBlank @Size(min = 8, max = 64) String password
) {
}
