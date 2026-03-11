package com.stableflow.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "PaymentInfoVo", description = "Invoice payment info / 账单支付信息")
public record PaymentInfoVo(
    @Schema(description = "Recipient wallet address / 收款地址")
    String recipientAddress,
    @Schema(description = "Unique invoice reference key / 唯一账单 reference", example = "ref_1234567890abcdef")
    String referenceKey,
    @Schema(description = "USDC mint address / USDC Mint 地址")
    String mintAddress,
    @Schema(description = "Expected payment amount / 应付金额", example = "99.00")
    BigDecimal expectedAmount,
    @Schema(description = "Solana payment link / Solana 支付链接")
    String paymentLink,
    @Schema(description = "Display label / 展示标签", example = "StableFlow Invoice")
    String label,
    @Schema(description = "Display message / 展示消息", example = "INV-20260311120000-ABCDEF12")
    String message,
    @Schema(description = "Invoice expiry time in UTC / 账单过期时间（UTC）")
    OffsetDateTime expireAt
) {
}
