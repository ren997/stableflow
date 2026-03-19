package com.stableflow.auth.vo;

import com.stableflow.merchant.enums.MerchantStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CurrentUserVo", description = "Current merchant info / 当前商家信息")
public record CurrentUserVo(
    @Schema(description = "Merchant id / 商家 ID", example = "1")
    Long merchantId,
    @Schema(description = "Merchant name / 商家名称", example = "StableFlow Demo")
    String merchantName,
    @Schema(description = "Merchant email / 商家邮箱", example = "demo@stableflow.com")
    String email,
    @Schema(description = "Merchant status / 商家状态", implementation = MerchantStatusEnum.class)
    MerchantStatusEnum status
) {
}
