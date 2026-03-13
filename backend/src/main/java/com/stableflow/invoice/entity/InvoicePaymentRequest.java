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

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Related invoice id / 关联账单 ID */
    private Long invoiceId;

    /** Snapshot recipient address / 收款地址快照 */
    private String recipientAddress;

    /** Unique payment reference key / 唯一支付 reference 标识 */
    private String referenceKey;

    /** Snapshot token mint address / 代币 Mint 地址快照 */
    private String mintAddress;

    /** Expected payment amount / 应付金额 */
    private BigDecimal expectedAmount;

    /** Generated Solana payment link / 生成的 Solana 支付链接 */
    private String paymentLink;

    /** Wallet display label / 钱包展示标签 */
    private String label;

    /** Wallet display message / 钱包展示消息 */
    private String message;

    /** Payment request expiry time in UTC / 支付请求过期时间（UTC） */
    private OffsetDateTime expireAt;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;
}
