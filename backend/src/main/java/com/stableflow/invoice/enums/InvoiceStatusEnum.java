package com.stableflow.invoice.enums;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Invoice lifecycle status enum / 账单生命周期状态枚举 */
@Getter
public enum InvoiceStatusEnum {
    /** Draft invoice not yet activated / 尚未激活的草稿账单 */
    DRAFT("DRAFT", "草稿"),
    /** Pending payment invoice / 待支付账单 */
    PENDING("PENDING", "待支付"),
    /** Fully paid invoice / 已足额支付账单 */
    PAID("PAID", "已支付"),
    /** Partially paid invoice / 部分支付账单 */
    PARTIALLY_PAID("PARTIALLY_PAID", "部分支付"),
    /** Overpaid invoice / 多付账单 */
    OVERPAID("OVERPAID", "超额支付"),
    /** Expired unpaid invoice / 已过期未支付账单 */
    EXPIRED("EXPIRED", "已过期"),
    /** Invoice with reconciliation failure / 核销失败账单 */
    FAILED_RECONCILIATION("FAILED_RECONCILIATION", "核销失败");

    private static final Map<String, InvoiceStatusEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(InvoiceStatusEnum::getCode, Function.identity()));

    /** Persisted status code / 持久化状态编码 */
    private final String code;

    /** Human-readable status description / 可读状态说明 */
    private final String desc;

    InvoiceStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by status code / 按状态编码解析枚举 */
    public static InvoiceStatusEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
