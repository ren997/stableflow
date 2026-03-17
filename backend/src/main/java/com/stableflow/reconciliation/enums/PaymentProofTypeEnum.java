package com.stableflow.reconciliation.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Payment proof type enum / 支付凭证类型枚举 */
@Getter
public enum PaymentProofTypeEnum {
    /** Invoice-level payment result snapshot / 账单级支付结果快照 */
    INVOICE_PAYMENT_RESULT("INVOICE_PAYMENT_RESULT", "账单支付结果快照");

    public static final String DESC = "支付凭证类型: INVOICE_PAYMENT_RESULT-账单支付结果快照";

    private static final Map<String, PaymentProofTypeEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(PaymentProofTypeEnum::getCode, Function.identity()));

    /** Persisted payment proof type code / 持久化支付凭证类型编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable payment proof type description / 可读支付凭证类型说明 */
    private final String desc;

    PaymentProofTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by payment proof type code / 按支付凭证类型编码解析枚举 */
    public static PaymentProofTypeEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
