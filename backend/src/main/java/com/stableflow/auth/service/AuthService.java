package com.stableflow.auth.service;

import com.stableflow.auth.dto.LoginRequestDto;
import com.stableflow.auth.dto.RegisterRequestDto;
import com.stableflow.auth.vo.CurrentUserVo;
import com.stableflow.auth.vo.LoginResponseVo;

public interface AuthService {

    /** Register a new merchant account and issue a JWT token / 注册新商家账号并签发 JWT token */
    LoginResponseVo register(RegisterRequestDto request);

    /** Authenticate merchant credentials and issue a JWT token / 校验商家凭证并签发 JWT token */
    LoginResponseVo login(LoginRequestDto request);

    /** Acknowledge logout for the current merchant and let the client clear its token / 确认当前商家登出并由客户端清理 token */
    void logout();

    /** Return the currently authenticated merchant summary / 返回当前登录商家摘要 */
    CurrentUserVo me();
}
