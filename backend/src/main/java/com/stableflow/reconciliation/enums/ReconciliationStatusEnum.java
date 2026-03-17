package com.stableflow.reconciliation.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Reconciliation processing status enum / 核销处理状态枚举 */
@Getter
public enum ReconciliationStatusEnum {
    /** Invoice status has been updated successfully / 已成功更新账单状态 */
    SUCCESS("SUCCESS", "核销成功"),
    /** Reconciliation was intentionally skipped because no invoice state change should happen / 核销被跳过 */
    SKIPPED("SKIPPED", "核销跳过"),
    /** Reconciliation failed due to an unexpected runtime problem / 核销失败 */
    FAILED("FAILED", "核销失败");

    public static final String DESC = "核销状态: SUCCESS-核销成功, SKIPPED-核销跳过, FAILED-核销失败";

    private static final Map<String, ReconciliationStatusEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(ReconciliationStatusEnum::getCode, Function.identity()));

    /** Persisted reconciliation status code / 持久化核销状态编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable reconciliation description / 可读核销状态说明 */
    private final String desc;

    ReconciliationStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by reconciliation code / 按核销状态编码解析枚举 */
    public static ReconciliationStatusEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
