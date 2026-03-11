package com.stableflow.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("merchant_payment_config")
public class MerchantPaymentConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String walletAddress;
    private String mintAddress;
    private String chain;
    private Boolean activeFlag;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
