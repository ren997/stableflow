package com.stableflow.dashboard.controller;

import com.stableflow.dashboard.dto.DashboardSummaryQueryDto;
import com.stableflow.dashboard.service.DashboardService;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.system.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard APIs / 仪表盘接口")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping("/summary")
    @Operation(summary = "Query dashboard summary / 查询仪表盘汇总数据")
    public ApiResponse<DashboardSummaryVo> getSummary(@Valid @RequestBody DashboardSummaryQueryDto request) {
        return ApiResponse.success(dashboardService.getSummary());
    }
}
