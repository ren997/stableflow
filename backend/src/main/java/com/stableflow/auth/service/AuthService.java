package com.stableflow.auth.service;

import com.stableflow.auth.dto.LoginRequestDto;
import com.stableflow.auth.vo.CurrentUserVo;
import com.stableflow.auth.vo.LoginResponseVo;

public interface AuthService {

    /** Authenticate merchant credentials and issue a JWT token / 校验商家凭证并签发 JWT token */
    LoginResponseVo login(LoginRequestDto request);

    /** Return the currently authenticated merchant summary / 返回当前登录商家摘要 */
    CurrentUserVo me();
}
