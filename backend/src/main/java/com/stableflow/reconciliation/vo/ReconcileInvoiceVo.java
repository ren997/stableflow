package com.stableflow.reconciliation.vo;

import com.stableflow.invoice.vo.PaymentStatusVo;
import io.swagger.v3.oas.annotations.media.Schema;

/** Manual reconcile result for one invoice / 单张账单手动核销结果 */
@Schema(name = "ReconcileInvoiceVo", description = "Manual invoice reconcile result / 手动账单核销结果")
public record ReconcileInvoiceVo(
    @Schema(description = "Invoice id / 账单 ID", example = "1")
    Long invoiceId,
    @Schema(description = "Number of newly reconciled transactions / 本次新核销交易数", example = "1")
    int reconciledCount,
    @Schema(description = "Latest payment status after manual reconcile / 手动核销后的最新支付状态")
    PaymentStatusVo paymentStatus
) {
}
