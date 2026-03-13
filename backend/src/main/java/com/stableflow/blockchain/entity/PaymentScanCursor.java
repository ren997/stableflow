package com.stableflow.blockchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("payment_scan_cursor")
public class PaymentScanCursor {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recipientAddress;
    private String lastSeenSignature;
    private OffsetDateTime lastScannedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
