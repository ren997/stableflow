package com.stableflow.invoice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("invoice_payment_request")
public class InvoicePaymentRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long invoiceId;
    private String recipientAddress;
    private String referenceKey;
    private String mintAddress;
    private BigDecimal expectedAmount;
    private String paymentLink;
    private String label;
    private String message;
    private OffsetDateTime expireAt;
    private OffsetDateTime createdAt;
}
