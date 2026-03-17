package com.stableflow.verification.enums;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Payment transaction business status enum / 支付交易业务状态枚举 */
@Getter
public enum PaymentTransactionStatusEnum {
    /** Newly scanned candidate transaction / 新扫描到的候选交易 */
    DETECTED("DETECTED", "已发现"),
    /** Transaction cannot be matched to a valid invoice / 交易暂时无法匹配有效账单 */
    UNMATCHED("UNMATCHED", "未匹配"),
    /** Transaction has been recognized as a valid full payment / 交易被识别为足额支付 */
    PAID("PAID", "已支付"),
    /** Transaction has been recognized as a partial payment / 交易被识别为部分支付 */
    PARTIALLY_PAID("PARTIALLY_PAID", "部分支付"),
    /** Transaction has been recognized as an overpayment / 交易被识别为超额支付 */
    OVERPAID("OVERPAID", "超额支付"),
    /** Transaction is treated as expired payment / 交易被识别为过期到账 */
    EXPIRED("EXPIRED", "已过期"),
    /** Transaction is treated as a duplicate payment / 交易被识别为重复支付 */
    DUPLICATE("DUPLICATE", "重复支付");

    private static final Map<String, PaymentTransactionStatusEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(PaymentTransactionStatusEnum::getCode, Function.identity()));

    /** Persisted payment status code / 持久化支付状态编码 */
    private final String code;

    /** Human-readable payment status description / 可读支付状态说明 */
    private final String desc;

    PaymentTransactionStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by payment status code / 按支付状态编码解析枚举 */
    public static PaymentTransactionStatusEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
