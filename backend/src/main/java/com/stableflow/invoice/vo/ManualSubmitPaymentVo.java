package com.stableflow.invoice.vo;

import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;

/** Manual payment submission result for one invoice / 单张账单手动支付提交结果 */
@Schema(name = "ManualSubmitPaymentVo", description = "Manual payment submit result / 手动支付提交结果")
public record ManualSubmitPaymentVo(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    Long invoiceId,
    @Schema(description = "Persisted payment transaction id / 支付交易 ID", example = "12")
    Long paymentTransactionId,
    @Schema(description = "Blockchain transaction hash / 链上交易哈希", example = "5N9rTxHash")
    String txHash,
    @Schema(description = "Parsed on-chain reference key if present / 链上解析出的 reference，若不存在则为空")
    String referenceKey,
    @Schema(description = "Verification result / 验证结果", implementation = PaymentVerificationResultEnum.class)
    PaymentVerificationResultEnum verificationResult,
    @Schema(description = "Derived payment transaction status / 派生支付交易状态", implementation = PaymentTransactionStatusEnum.class)
    PaymentTransactionStatusEnum paymentTransactionStatus,
    @Schema(description = "Number of newly reconciled records in this submission / 本次新增核销数", example = "1")
    int reconciledCount,
    @Schema(description = "Latest invoice payment status snapshot / 最新账单支付状态快照")
    PaymentStatusVo paymentStatus,
    @Schema(description = "Result message / 处理结果说明")
    String message
) {
}
