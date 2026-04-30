package com.stableflow.system.controller;

import com.stableflow.system.api.ApiResponse;
import com.stableflow.system.service.SystemRuntimeConfigService;
import com.stableflow.system.vo.SystemRuntimeConfigVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Runtime and infrastructure helpers / 运行时与基础设施辅助接口")
public class SystemController {

    private final SystemRuntimeConfigService systemRuntimeConfigService;

    @PostMapping("/runtime-config")
    @Operation(summary = "Get runtime config / 获取运行时配置")
    public ApiResponse<SystemRuntimeConfigVo> getRuntimeConfig() {
        return ApiResponse.success(systemRuntimeConfigService.getRuntimeConfig());
    }
}
