package com.stableflow.blockchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName(value = "payment_transaction", autoResultMap = true)
public class PaymentTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long invoiceId;
    private String txHash;
    private String referenceKey;
    private String payerAddress;
    private String recipientAddress;
    private BigDecimal amount;
    private String currency;
    private String mintAddress;
    private OffsetDateTime blockTime;
    private String verificationResult;
    private String paymentStatus;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode rawPayload;
    private OffsetDateTime createdAt;
}
