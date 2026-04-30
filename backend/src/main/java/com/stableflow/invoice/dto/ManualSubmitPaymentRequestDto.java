package com.stableflow.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Manual payment submission request scoped to one invoice / 面向单张账单的手动支付提交请求 */
@Schema(name = "ManualSubmitPaymentRequestDto", description = "Manual payment submit request / 手动支付提交请求")
public record ManualSubmitPaymentRequestDto(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    @NotNull(message = "Invoice id is required")
    Long invoiceId,
    @Schema(description = "Blockchain transaction hash / 链上交易哈希", example = "5N9rTxHash")
    @NotBlank(message = "Transaction hash is required")
    String txHash
) {
}
