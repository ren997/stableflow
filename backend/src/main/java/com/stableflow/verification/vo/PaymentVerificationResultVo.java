package com.stableflow.verification.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaymentVerificationResultVo", description = "Payment verification result / 支付验证结果")
public record PaymentVerificationResultVo(
    @Schema(description = "Payment transaction id / 支付交易 ID", example = "1")
    Long paymentTransactionId,
    @Schema(description = "Matched invoice id / 匹配到的账单 ID", example = "1001")
    Long invoiceId,
    @Schema(description = "Blockchain transaction hash / 链上交易哈希", example = "5N9rTxHash")
    String txHash,
    @Schema(description = "Parsed reference key / 解析出的 reference 标识", example = "ref_1234567890abcdef")
    String referenceKey,
    @Schema(description = "Verification result code / 验证结果代码", example = "PAID")
    String verificationResult,
    @Schema(description = "Derived payment status / 派生支付状态", example = "PAID")
    String paymentStatus,
    @Schema(description = "Verification message / 验证说明", example = "Matched invoice payment successfully.")
    String message
) {
}
