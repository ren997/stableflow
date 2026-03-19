package com.stableflow.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "DashboardSummaryVo", description = "Dashboard summary response / 仪表盘汇总返回")
public record DashboardSummaryVo(
    @Schema(description = "Total invoice count / 总账单数", example = "120")
    long totalInvoices,
    @Schema(description = "Paid invoice count / 已支付账单数", example = "85")
    long paidCount,
    @Schema(description = "Unpaid invoice count (DRAFT + PENDING) / 待支付账单数", example = "20")
    long unpaidCount,
    @Schema(description = "Exception invoice count / 异常账单数", example = "15")
    long exceptionCount,
    @Schema(description = "Total verified on-chain received amount / 链上已验证收款总额", example = "8520.50")
    BigDecimal totalReceivedAmount
) {
}
