package com.stableflow.invoice.vo;

import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** Payment status response / 支付状态返回对象 */
@Schema(name = "PaymentStatusVo", description = "Invoice payment status response / 账单支付状态返回")
public record PaymentStatusVo(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    Long invoiceId,
    @Schema(description = "Public invoice id / 公开账单标识", example = "pub_1234567890abcdef")
    String publicId,
    @Schema(description = "Invoice number / 账单编号", example = "INV-20260311120000-ABCDEF12")
    String invoiceNo,
    @Schema(description = "Final invoice status / 最终账单状态", implementation = InvoiceStatusEnum.class)
    InvoiceStatusEnum status,
    @Schema(description = "Exception tags / 异常标签")
    List<String> exceptionTags,
    @Schema(description = "Invoice paid time in UTC / 账单支付时间（UTC）")
    OffsetDateTime paidAt,
    @Schema(description = "Latest processed time in UTC / 最近处理时间（UTC）")
    OffsetDateTime lastProcessedAt,
    @Schema(description = "Latest transaction hash / 最近交易哈希")
    String latestTxHash,
    @Schema(description = "Latest verification result / 最近验证结果", implementation = PaymentVerificationResultEnum.class)
    PaymentVerificationResultEnum latestVerificationResult,
    @Schema(description = "Latest payment transaction status / 最近支付交易状态", implementation = PaymentTransactionStatusEnum.class)
    PaymentTransactionStatusEnum latestPaymentStatus
) {
}
