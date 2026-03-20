package com.stableflow.dashboard.controller;

import com.stableflow.dashboard.dto.DashboardExceptionInvoiceQueryDto;
import com.stableflow.dashboard.dto.DashboardSummaryQueryDto;
import com.stableflow.dashboard.service.DashboardService;
import com.stableflow.dashboard.vo.DashboardExceptionInvoiceVo;
import com.stableflow.dashboard.vo.DashboardInvoiceStatusDistributionVo;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.system.api.PageResult;
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
    public ApiResponse<DashboardSummaryVo> getSummary(@Valid @RequestBody(required = false) DashboardSummaryQueryDto request) {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @PostMapping("/invoices/status")
    @Operation(summary = "Query dashboard invoice status distribution / 查询仪表盘账单状态分布")
    public ApiResponse<DashboardInvoiceStatusDistributionVo> getInvoiceStatusDistribution() {
        return ApiResponse.success(dashboardService.getInvoiceStatusDistribution());
    }

    @PostMapping("/invoices/exceptions")
    @Operation(summary = "Query dashboard exception invoices / 查询仪表盘异常账单")
    public ApiResponse<PageResult<DashboardExceptionInvoiceVo>> getExceptionInvoices(
        @Valid @RequestBody(required = false) DashboardExceptionInvoiceQueryDto request
    ) {
        int page = request == null || request.page() == null ? 1 : request.page();
        int size = request == null || request.size() == null ? 20 : request.size();
        return ApiResponse.success(
            dashboardService.getExceptionInvoices(
                request == null ? null : request.exceptionTag(),
                page,
                size
            )
        );
    }
}
