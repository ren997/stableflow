package com.stableflow.dashboard.dto;

import com.stableflow.invoice.enums.ExceptionTagEnum;
import io.swagger.v3.oas.annotations.media.Schema;

/** Dashboard exception invoice query request / 仪表盘异常账单查询请求 */
@Schema(name = "DashboardExceptionInvoiceQueryDto", description = "Dashboard exception invoice query request / 仪表盘异常账单查询请求")
public record DashboardExceptionInvoiceQueryDto(
    @Schema(description = "Exception tag filter / 异常标签筛选", implementation = ExceptionTagEnum.class)
    ExceptionTagEnum exceptionTag,
    @Schema(description = "Page number (1-based) / 页码（从 1 开始）", example = "1")
    Integer page,
    @Schema(description = "Page size / 每页条数", example = "20")
    Integer size
) {
}
