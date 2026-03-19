package com.stableflow.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Invoice activation request / 账单激活请求 */
@Schema(name = "ActivateInvoiceRequestDto", description = "Invoice activation request / 账单激活请求")
public record ActivateInvoiceRequestDto(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    @NotNull Long id
) {
}
