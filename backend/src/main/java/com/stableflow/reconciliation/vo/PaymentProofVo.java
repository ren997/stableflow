package com.stableflow.reconciliation.vo;

import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.reconciliation.enums.ReconciliationStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Payment proof response / 支付凭证返回对象 */
@Schema(name = "PaymentProofVo", description = "Invoice payment proof response / 账单支付凭证返回")
public record PaymentProofVo(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    Long invoiceId,
    @Schema(description = "Public invoice id / 公开账单标识", example = "pub_1234567890abcdef")
    String publicId,
    @Schema(description = "Invoice number / 账单编号", example = "INV-20260311120000-ABCDEF12")
    String invoiceNo,
    @Schema(description = "Blockchain transaction hash / 链上交易哈希")
    String txHash,
    @Schema(description = "Invoice reference key / 账单 reference", example = "ref_1234567890abcdef")
    String referenceKey,
    @Schema(description = "Payer wallet address / 付款地址")
    String payerAddress,
    @Schema(description = "Recipient wallet address / 收款地址")
    String recipientAddress,
    @Schema(description = "Token mint address / 代币 Mint 地址")
    String mintAddress,
    @Schema(description = "Paid amount / 支付金额", example = "99.00")
    BigDecimal amount,
    @Schema(description = "Paid time in UTC / 支付时间（UTC）")
    OffsetDateTime paidAt,
    @Schema(description = "Payment verification result / 支付验证结果", implementation = PaymentVerificationResultEnum.class)
    PaymentVerificationResultEnum verificationResult,
    @Schema(description = "Invoice final status / 账单最终状态", implementation = InvoiceStatusEnum.class)
    InvoiceStatusEnum finalStatus,
    @Schema(description = "Exception tags / 异常标签", implementation = ExceptionTagEnum.class)
    List<ExceptionTagEnum> exceptionTags,
    @Schema(description = "Reconciliation status / 核销状态", implementation = ReconciliationStatusEnum.class)
    ReconciliationStatusEnum reconciliationStatus,
    @Schema(description = "Reconciliation result message / 核销结果说明")
    String resultMessage,
    @Schema(description = "Proof snapshot created time in UTC / 凭证快照创建时间（UTC）")
    OffsetDateTime createdAt
) {
}
