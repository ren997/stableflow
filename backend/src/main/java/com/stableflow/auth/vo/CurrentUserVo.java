package com.stableflow.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CurrentUserVo", description = "Current merchant info / 当前商家信息")
public record CurrentUserVo(
    @Schema(description = "Merchant id / 商家 ID", example = "1")
    Long merchantId,
    @Schema(description = "Merchant email / 商家邮箱", example = "demo@stableflow.com")
    String email
) {
}
