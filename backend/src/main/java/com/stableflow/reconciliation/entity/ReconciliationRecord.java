package com.stableflow.reconciliation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.reconciliation.enums.ReconciliationStatusEnum;
import java.time.OffsetDateTime;
import lombok.Data;

/** Reconciliation record created after a verified transaction is consumed by invoice state updates / 已验证交易被核销并更新账单状态后生成的核销记录实体 */
@Data
@TableName(value = "reconciliation_record", autoResultMap = true)
public class ReconciliationRecord {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Related invoice id / 关联账单 ID */
    private Long invoiceId;

    /** Blockchain transaction hash / 链上交易哈希 */
    private String txHash;

    /** Reconciliation status / 核销状态 */
    private ReconciliationStatusEnum reconciliationStatus;

    /** Human-readable reconciliation message / 可读核销结果说明 */
    private String resultMessage;

    /** Exception tag list stored as JSON / 以 JSON 形式存储的异常标签列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode exceptionTags;

    /** Reconciliation processed time in UTC / 核销处理时间（UTC） */
    private OffsetDateTime processedAt;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;
}
