package com.stableflow.outbox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.stableflow.outbox.enums.OutboxAggregateTypeEnum;
import com.stableflow.outbox.enums.OutboxEventStatusEnum;
import com.stableflow.outbox.enums.OutboxEventTypeEnum;
import com.stableflow.system.persistence.JsonNodeJsonbTypeHandler;
import java.time.OffsetDateTime;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

/** Reliable outbox event persisted for asynchronous downstream dispatch / 为后续异步分发持久化的可靠 outbox 事件实体 */
@Data
@TableName(value = "outbox_event", autoResultMap = true)
public class OutboxEvent {

    /** Primary key / 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Event type code / 事件类型编码 */
    private OutboxEventTypeEnum eventType;

    /** Aggregate type code / 聚合根类型编码 */
    private OutboxAggregateTypeEnum aggregateType;

    /** Aggregate identifier / 聚合根标识 */
    private String aggregateId;

    /** Structured event payload stored as JSON / 以 JSON 形式存储的结构化事件载荷 */
    @TableField(typeHandler = JsonNodeJsonbTypeHandler.class, jdbcType = JdbcType.OTHER)
    private JsonNode payload;

    /** Dispatch status / 分发状态 */
    private OutboxEventStatusEnum status;

    /** Retry count / 重试次数 */
    private Integer retryCount;

    /** Last dispatch error message / 最近一次分发错误信息 */
    private String lastError;

    /** Next retry time in UTC / 下次重试时间（UTC） */
    private OffsetDateTime nextRetryAt;

    /** Record created time in UTC / 记录创建时间（UTC） */
    private OffsetDateTime createdAt;

    /** Record updated time in UTC / 记录更新时间（UTC） */
    private OffsetDateTime updatedAt;
}
