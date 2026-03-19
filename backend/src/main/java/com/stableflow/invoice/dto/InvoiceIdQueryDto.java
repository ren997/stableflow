package com.stableflow.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Invoice id query request / 账单 ID 查询请求 */
@Schema(name = "InvoiceIdQueryDto", description = "Invoice id query request / 账单 ID 查询请求")
public record InvoiceIdQueryDto(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    @NotNull Long id
) {
}
