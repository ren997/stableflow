package com.stableflow.blockchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("payment_scan_cursor")
public class PaymentScanCursor {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Recipient wallet address being scanned / 正在扫描的收款钱包地址 */
    private String recipientAddress;

    /** Latest processed transaction signature / 最近一次已处理的交易签名 */
    private String lastSeenSignature;

    /** Last completed scan time in UTC / 最近一次扫描完成时间（UTC） */
    private OffsetDateTime lastScannedAt;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;

    /** Record updated time in UTC / 记录更新时间（UTC） */
    private OffsetDateTime updatedAt;
}
