package com.stableflow.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponseVo", description = "Merchant login response / 商家登录返回")
public record LoginResponseVo(
    @Schema(description = "JWT access token / JWT 访问令牌")
    String accessToken,
    @Schema(description = "Token type / 令牌类型", example = "Bearer")
    String tokenType,
    @Schema(description = "Merchant id / 商家 ID", example = "1")
    Long merchantId,
    @Schema(description = "Merchant email / 商家邮箱", example = "demo@stableflow.com")
    String email,
    @Schema(description = "Merchant display name / 商家名称", example = "StableFlow Demo")
    String merchantName
) {
}
