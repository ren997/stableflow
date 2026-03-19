package com.stableflow.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.stableflow.auth.dto.LoginRequestDto;
import com.stableflow.auth.dto.RegisterRequestDto;
import com.stableflow.auth.service.AuthService;
import com.stableflow.auth.vo.CurrentUserVo;
import com.stableflow.auth.vo.LoginResponseVo;
import com.stableflow.system.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication APIs / 认证接口")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Merchant register / 商家注册")
    public ApiResponse<LoginResponseVo> register(@Valid @RequestBody RegisterRequestDto request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Merchant login / 商家登录")
    public ApiResponse<LoginResponseVo> login(@Valid @RequestBody LoginRequestDto request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current merchant info / 获取当前商家信息")
    public ApiResponse<CurrentUserVo> me() {
        return ApiResponse.success(authService.me());
    }
}
