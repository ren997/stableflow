package com.stableflow.invoice.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentInfoVo(
    String recipientAddress,
    String referenceKey,
    String mintAddress,
    BigDecimal expectedAmount,
    String paymentLink,
    String label,
    String message,
    OffsetDateTime expireAt
) {
}
