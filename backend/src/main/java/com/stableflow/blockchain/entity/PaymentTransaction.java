package com.stableflow.blockchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/** Candidate on-chain payment transaction discovered before verification / 支付验证前通过链上扫描发现的候选支付交易实体 */
@Data
@TableName(value = "payment_transaction", autoResultMap = true)
public class PaymentTransaction {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Matched invoice id if already associated / 已关联时对应的账单 ID */
    private Long invoiceId;

    /** Blockchain transaction hash / 链上交易哈希 */
    private String txHash;

    /** Parsed reference key from the transaction / 从交易中解析出的 reference 标识 */
    private String referenceKey;

    /** Payer wallet address / 付款钱包地址 */
    private String payerAddress;

    /** Recipient wallet or token account address / 收款钱包或 token 账户地址 */
    private String recipientAddress;

    /** Parsed transfer amount / 解析出的转账金额 */
    private BigDecimal amount;

    /** Normalized currency code / 归一化后的币种代码 */
    private String currency;

    /** Token mint address / 代币 Mint 地址 */
    private String mintAddress;

    /** On-chain block time in UTC / 链上区块时间（UTC） */
    private OffsetDateTime blockTime;

    /** Verification result status / 验证结果状态 */
    private PaymentVerificationResultEnum verificationResult;

    /** Derived payment status / 派生支付状态 */
    private PaymentTransactionStatusEnum paymentStatus;

    /** Raw blockchain payload stored as JSON / 以 JSON 形式保存的原始链上载荷 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode rawPayload;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;
}
