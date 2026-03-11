package com.stableflow.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "InvoiceDetailVo", description = "Invoice detail response / 账单详情返回")
public record InvoiceDetailVo(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    Long id,
    @Schema(description = "Public invoice id / 公开账单标识", example = "pub_1234567890abcdef")
    String publicId,
    @Schema(description = "Invoice number / 账单编号", example = "INV-20260311120000-ABCDEF12")
    String invoiceNo,
    @Schema(description = "Customer name / 客户名称", example = "Alice")
    String customerName,
    @Schema(description = "Invoice amount / 账单金额", example = "99.00")
    BigDecimal amount,
    @Schema(description = "Currency code / 币种代码", example = "USDC")
    String currency,
    @Schema(description = "Blockchain name / 链名称", example = "SOLANA")
    String chain,
    @Schema(description = "Invoice description / 账单描述")
    String description,
    @Schema(description = "Invoice status / 账单状态", example = "PENDING")
    String status,
    @Schema(description = "Invoice expiry time in UTC / 账单过期时间（UTC）")
    OffsetDateTime expireAt,
    @Schema(description = "Paid time in UTC / 支付时间（UTC）")
    OffsetDateTime paidAt,
    @Schema(description = "Created time in UTC / 创建时间（UTC）")
    OffsetDateTime createdAt,
    @Schema(description = "Payment info / 支付信息")
    PaymentInfoVo paymentInfo
) {
}
