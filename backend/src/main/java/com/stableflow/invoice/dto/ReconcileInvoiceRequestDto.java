package com.stableflow.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Manual reconcile request for one invoice / 单张账单手动触发核销请求 */
@Schema(name = "ReconcileInvoiceRequestDto", description = "Manual invoice reconcile request / 手动账单核销请求")
public record ReconcileInvoiceRequestDto(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    @NotNull(message = "Invoice id is required")
    Long id
) {
}
