package com.stableflow.outbox.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Aggregate type enum used by reliable outbox events / 可靠 outbox 事件使用的聚合根类型枚举 */
@Getter
public enum OutboxAggregateTypeEnum {
    /** Invoice aggregate / 账单聚合 */
    INVOICE("INVOICE", "账单");

    public static final String DESC = "Outbox 聚合根类型: INVOICE-账单";

    private static final Map<String, OutboxAggregateTypeEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(OutboxAggregateTypeEnum::getCode, Function.identity()));

    /** Persisted aggregate type code / 持久化聚合根类型编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable aggregate type description / 可读聚合根类型说明 */
    private final String desc;

    OutboxAggregateTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by aggregate type code / 按聚合根类型编码解析枚举 */
    public static OutboxAggregateTypeEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
