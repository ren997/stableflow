package com.stableflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequestDto", description = "Merchant login request / 商家登录请求")
public record LoginRequestDto(
    @Schema(description = "Merchant email / 商家邮箱", example = "demo@stableflow.com")
    @Email @NotBlank @Size(max = 255) String email,
    @Schema(description = "Merchant password / 商家密码", example = "Password123")
    @NotBlank @Size(max = 64) String password
) {
}
