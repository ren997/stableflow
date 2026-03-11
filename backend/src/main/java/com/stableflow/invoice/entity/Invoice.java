package com.stableflow.invoice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("invoice")
public class Invoice {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String publicId;
    private Long merchantId;
    private String invoiceNo;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private String chain;
    private String description;
    private String status;
    private String exceptionTags;
    private OffsetDateTime expireAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
