package com.stableflow.auth.vo;

public record LoginResponseVo(
    String accessToken,
    String tokenType,
    Long merchantId,
    String email,
    String merchantName
) {
}
