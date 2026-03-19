package com.stableflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Current merchant profile query request / 当前登录商家信息查询请求 */
@Schema(name = "CurrentUserRequestDto", description = "Current merchant profile query request / 当前登录商家信息查询请求")
public record CurrentUserRequestDto() {
}
