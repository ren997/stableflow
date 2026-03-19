package com.stableflow.dashboard.controller;

import com.stableflow.dashboard.service.DashboardService;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.system.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard APIs / 仪表盘接口")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary / 获取仪表盘汇总数据")
    public ApiResponse<DashboardSummaryVo> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }
}
