package com.stableflow.verification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Payment verification result enum / 支付验证结果枚举 */
@Getter
public enum PaymentVerificationResultEnum {
    /** Transaction has been detected but not verified yet / 已发现但尚未完成验证 */
    PENDING("PENDING", "待验证"),
    /** Transaction does not contain a usable reference key / 交易缺少可用 reference */
    MISSING_REFERENCE("MISSING_REFERENCE", "缺少 reference"),
    /** Transaction reference cannot match a payment request / 交易 reference 无法匹配支付请求 */
    INVALID_REFERENCE("INVALID_REFERENCE", "reference 无效"),
    /** Transaction mint does not match the expected currency / 交易币种不匹配 */
    WRONG_CURRENCY("WRONG_CURRENCY", "币种错误"),
    /** Transaction amount exactly matches the invoice amount / 交易金额与账单金额一致 */
    PAID("PAID", "已支付"),
    /** Transaction amount is lower than the invoice amount / 交易金额小于账单金额 */
    PARTIALLY_PAID("PARTIALLY_PAID", "部分支付"),
    /** Transaction amount is greater than the invoice amount / 交易金额大于账单金额 */
    OVERPAID("OVERPAID", "超额支付"),
    /** Transaction arrives after the payment request expires / 交易在支付请求过期后到账 */
    LATE_PAYMENT("LATE_PAYMENT", "过期到账"),
    /** A prior effective transaction already exists / 已存在更早的有效支付 */
    DUPLICATE_PAYMENT("DUPLICATE_PAYMENT", "重复支付");

    public static final String DESC =
        "支付验证结果: PENDING-待验证, MISSING_REFERENCE-缺少 reference, INVALID_REFERENCE-reference 无效, WRONG_CURRENCY-币种错误, PAID-已支付, PARTIALLY_PAID-部分支付, OVERPAID-超额支付, LATE_PAYMENT-过期到账, DUPLICATE_PAYMENT-重复支付";

    private static final Map<String, PaymentVerificationResultEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(PaymentVerificationResultEnum::getCode, Function.identity()));

    /** Persisted verification result code / 持久化验证结果编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable verification description / 可读验证结果说明 */
    private final String desc;

    PaymentVerificationResultEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by verification code / 按验证结果编码解析枚举 */
    public static PaymentVerificationResultEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
