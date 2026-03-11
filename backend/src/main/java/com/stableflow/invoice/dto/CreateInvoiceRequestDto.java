package com.stableflow.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateInvoiceRequestDto(
    @NotBlank String customerName,
    @NotNull @DecimalMin("0.000001") BigDecimal amount,
    String currency,
    String chain,
    String description,
    @NotNull @Future OffsetDateTime expireAt
) {
}
