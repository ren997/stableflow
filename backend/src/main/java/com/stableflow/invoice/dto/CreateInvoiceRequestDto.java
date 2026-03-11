package com.stableflow.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "CreateInvoiceRequestDto", description = "Create invoice request / 创建账单请求")
public record CreateInvoiceRequestDto(
    @Schema(description = "Customer name / 客户名称", example = "Alice")
    @NotBlank String customerName,
    @Schema(description = "Expected payment amount / 应付金额", example = "99.00")
    @NotNull @DecimalMin("0.000001") BigDecimal amount,
    @Schema(description = "Currency code / 币种代码", example = "USDC")
    String currency,
    @Schema(description = "Blockchain name / 链名称", example = "SOLANA")
    String chain,
    @Schema(description = "Invoice description / 账单描述", example = "Monthly subscription fee")
    String description,
    @Schema(description = "Invoice expiry time in UTC / 账单过期时间（UTC）", example = "2026-03-18T12:00:00Z")
    @NotNull @Future OffsetDateTime expireAt
) {
}
