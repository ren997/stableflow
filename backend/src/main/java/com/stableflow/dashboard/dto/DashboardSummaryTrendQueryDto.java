package com.stableflow.dashboard.dto;

import com.stableflow.dashboard.enums.DashboardTimeGranularityEnum;
import io.swagger.v3.oas.annotations.media.Schema;

/** Dashboard summary trend query request / 仪表盘汇总趋势查询请求 */
@Schema(name = "DashboardSummaryTrendQueryDto", description = "Dashboard summary trend query request / 仪表盘汇总趋势查询请求")
public record DashboardSummaryTrendQueryDto(
    @Schema(description = "Time granularity / 时间粒度", implementation = DashboardTimeGranularityEnum.class)
    DashboardTimeGranularityEnum granularity
) {
}
