package com.stableflow.outbox.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Dispatch lifecycle status enum for reliable outbox events / 可靠 outbox 事件的分发生命周期状态枚举 */
@Getter
public enum OutboxEventStatusEnum {
    /** Newly created event waiting for dispatch / 新建待分发事件 */
    PENDING("PENDING", "待分发"),
    /** Event currently being dispatched / 事件分发中 */
    DISPATCHING("DISPATCHING", "分发中"),
    /** Event dispatch completed successfully / 事件分发成功 */
    DISPATCHED("DISPATCHED", "已分发"),
    /** Event dispatch failed and is waiting for retry / 事件分发失败待重试 */
    FAILED("FAILED", "分发失败");

    public static final String DESC =
        "Outbox 分发状态: PENDING-待分发, DISPATCHING-分发中, DISPATCHED-已分发, FAILED-分发失败";

    private static final Map<String, OutboxEventStatusEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(OutboxEventStatusEnum::getCode, Function.identity()));

    /** Persisted event status code / 持久化事件状态编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable event status description / 可读事件状态说明 */
    private final String desc;

    OutboxEventStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by event status code / 按事件状态编码解析枚举 */
    public static OutboxEventStatusEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
