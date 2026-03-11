package com.stableflow.auth.vo;

public record CurrentUserVo(
    Long merchantId,
    String email
) {
}
