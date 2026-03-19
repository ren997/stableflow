package com.stableflow.invoice.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Exception tag code dictionary for invoice and reconciliation views / 账单与核销视图使用的异常标签编码字典 */
@Getter
public enum ExceptionTagEnum {
    /** Payment arrives after invoice expiry / 过期到账 */
    LATE_PAYMENT("LATE_PAYMENT", "过期到账"),
    /** Payment token does not match expected currency / 币种错误 */
    WRONG_CURRENCY("WRONG_CURRENCY", "币种错误"),
    /** Additional payment is received after an effective payment already exists / 重复支付 */
    DUPLICATE_PAYMENT("DUPLICATE_PAYMENT", "重复支付"),
    /** Payment cannot be matched to any invoice / 未识别付款 */
    UNMATCHED_PAYMENT("UNMATCHED_PAYMENT", "未识别付款"),
    /** Payment does not carry a usable reference / 缺少 reference */
    MISSING_REFERENCE("MISSING_REFERENCE", "缺少 reference"),
    /** Payment carries an invalid reference / reference 无效 */
    INVALID_REFERENCE("INVALID_REFERENCE", "reference 无效"),
    /** Payment confirmation or arrival is delayed / 到账延迟 */
    PAYMENT_DELAYED("PAYMENT_DELAYED", "到账延迟");

    public static final String DESC =
        "异常标签: LATE_PAYMENT-过期到账, WRONG_CURRENCY-币种错误, DUPLICATE_PAYMENT-重复支付, UNMATCHED_PAYMENT-未识别付款, MISSING_REFERENCE-缺少 reference, INVALID_REFERENCE-reference 无效, PAYMENT_DELAYED-到账延迟";

    private static final Map<String, ExceptionTagEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(ExceptionTagEnum::getCode, Function.identity()));

    /** Exception tag code / 异常标签编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Exception tag description / 异常标签说明 */
    private final String desc;

    ExceptionTagEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by exception tag code / 按异常标签编码解析枚举 */
    public static ExceptionTagEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
