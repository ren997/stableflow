package com.stableflow.dashboard.vo;

import com.stableflow.dashboard.enums.DashboardTimeGranularityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Dashboard summary trend response / 仪表盘汇总趋势返回 */
@Schema(name = "DashboardSummaryTrendVo", description = "Dashboard summary trend response / 仪表盘汇总趋势返回")
public record DashboardSummaryTrendVo(
    @Schema(description = "Time granularity / 时间粒度", implementation = DashboardTimeGranularityEnum.class)
    DashboardTimeGranularityEnum granularity,
    @Schema(description = "Trend points / 趋势点列表")
    List<TrendPoint> items
) {
    /** Dashboard trend point / 仪表盘趋势点 */
    @Schema(name = "DashboardSummaryTrendPoint", description = "Dashboard summary trend point / 仪表盘汇总趋势点")
    public record TrendPoint(
        @Schema(description = "Bucket start time in UTC / 时间桶开始时间（UTC）")
        OffsetDateTime bucketStartAt,
        @Schema(description = "Total received amount for the bucket / 时间桶收款总额", example = "99.50")
        BigDecimal totalReceivedAmount,
        @Schema(description = "Verified transaction count for the bucket / 时间桶已验证交易数", example = "3")
        long transactionCount
    ) {
    }
}
