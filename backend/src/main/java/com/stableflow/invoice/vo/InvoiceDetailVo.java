package com.stableflow.invoice.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record InvoiceDetailVo(
    Long id,
    String publicId,
    String invoiceNo,
    String customerName,
    BigDecimal amount,
    String currency,
    String chain,
    String description,
    String status,
    OffsetDateTime expireAt,
    OffsetDateTime paidAt,
    OffsetDateTime createdAt,
    PaymentInfoVo paymentInfo
) {
}
