package com.stableflow.outbox.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Business event type enum emitted through reliable outbox / 通过可靠 outbox 发出的业务事件类型枚举 */
@Getter
public enum OutboxEventTypeEnum {
    /** Invoice payment result is finalized by reconciliation / 账单支付结果在核销后被最终确认 */
    INVOICE_PAYMENT_RESULT("INVOICE_PAYMENT_RESULT", "账单支付结果");

    public static final String DESC = "Outbox 事件类型: INVOICE_PAYMENT_RESULT-账单支付结果";

    private static final Map<String, OutboxEventTypeEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(OutboxEventTypeEnum::getCode, Function.identity()));

    /** Persisted event type code / 持久化事件类型编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable event type description / 可读事件类型说明 */
    private final String desc;

    OutboxEventTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by event type code / 按事件类型编码解析枚举 */
    public static OutboxEventTypeEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
