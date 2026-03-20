package com.stableflow.dashboard.vo;

import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Dashboard exception invoice item / 仪表盘异常账单项 */
@Schema(name = "DashboardExceptionInvoiceVo", description = "Dashboard exception invoice item / 仪表盘异常账单项")
public record DashboardExceptionInvoiceVo(
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
    @Schema(description = "Invoice status / 账单状态", implementation = InvoiceStatusEnum.class)
    InvoiceStatusEnum status,
    @Schema(description = "Exception tags / 异常标签", implementation = ExceptionTagEnum.class)
    List<ExceptionTagEnum> exceptionTags,
    @Schema(description = "Invoice expiry time in UTC / 账单过期时间（UTC）")
    OffsetDateTime expireAt,
    @Schema(description = "Created time in UTC / 创建时间（UTC）")
    OffsetDateTime createdAt
) {
}
