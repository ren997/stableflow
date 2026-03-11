package com.stableflow.merchant.vo;

import java.time.OffsetDateTime;

public record MerchantPaymentConfigVo(
    Long id,
    Long merchantId,
    String walletAddress,
    String mintAddress,
    String chain,
    Boolean activeFlag,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
