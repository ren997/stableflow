package com.stableflow.invoice.dto;

import com.stableflow.invoice.enums.ExceptionTagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/** Invoice list query request / 账单列表查询请求 */
@Schema(name = "InvoiceListQueryDto", description = "Invoice list query request / 账单列表查询请求")
public record InvoiceListQueryDto(
    @Schema(description = "Invoice status filter / 账单状态筛选", example = "PENDING")
    String status,
    @Schema(description = "Exception tag filter / 异常标签筛选", implementation = ExceptionTagEnum.class)
    ExceptionTagEnum exceptionTag,
    @Schema(description = "Page number (1-based) / 页码（从 1 开始）", example = "1")
    @Min(1) Integer page,
    @Schema(description = "Page size / 每页条数", example = "20")
    @Min(1) Integer size
) {
}
