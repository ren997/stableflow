package com.stableflow.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.reconciliation.enums.PaymentProofTypeEnum;
import java.time.OffsetDateTime;
import lombok.Data;

/** Snapshot record that stores invoice-level payment proof details / 存储账单级支付证明详情的快照实体 */
@Data
@TableName(value = "payment_proof", autoResultMap = true)
public class PaymentProof {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Related invoice id / 关联账单 ID */
    private Long invoiceId;

    /** Blockchain transaction hash / 链上交易哈希 */
    private String txHash;

    /** Payment proof type / 支付凭证类型 */
    private PaymentProofTypeEnum proofType;

    /** Structured proof payload stored as JSON / 以 JSON 形式存储的结构化凭证载荷 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode proofPayload;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;
}
