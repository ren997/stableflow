package com.stableflow.invoice.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record InvoiceListItemVo(
    Long id,
    String publicId,
    String invoiceNo,
    String customerName,
    BigDecimal amount,
    String currency,
    String status,
    OffsetDateTime expireAt,
    OffsetDateTime createdAt
) {
}
