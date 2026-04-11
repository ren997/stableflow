package com.stableflow.invoice.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
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
    /** Cancelled invoice removed from payment flow / 已取消且不再进入支付流程的账单 */
    CANCELLED("CANCELLED", "已取消"),
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

    public static final String DESC =
        "账单状态: DRAFT-草稿, PENDING-待支付, CANCELLED-已取消, PAID-已支付, PARTIALLY_PAID-部分支付, OVERPAID-超额支付, EXPIRED-已过期, FAILED_RECONCILIATION-核销失败";

    private static final Map<String, InvoiceStatusEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(InvoiceStatusEnum::getCode, Function.identity()));

    /** Persisted status code / 持久化状态编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable status description / 可读状态说明 */
    private final String desc;

    /** Allowed next statuses from the current status / 当前状态允许迁移到的目标状态 */
    private Set<InvoiceStatusEnum> allowedTransitions;

    InvoiceStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    static {
        DRAFT.allowedTransitions = EnumSet.of(PENDING, CANCELLED);
        PENDING.allowedTransitions = EnumSet.of(CANCELLED, PAID, PARTIALLY_PAID, OVERPAID, EXPIRED, FAILED_RECONCILIATION);
        CANCELLED.allowedTransitions = EnumSet.noneOf(InvoiceStatusEnum.class);
        PAID.allowedTransitions = EnumSet.noneOf(InvoiceStatusEnum.class);
        PARTIALLY_PAID.allowedTransitions = EnumSet.noneOf(InvoiceStatusEnum.class);
        OVERPAID.allowedTransitions = EnumSet.noneOf(InvoiceStatusEnum.class);
        EXPIRED.allowedTransitions = EnumSet.of(PAID, PARTIALLY_PAID, OVERPAID, FAILED_RECONCILIATION);
        FAILED_RECONCILIATION.allowedTransitions = EnumSet.of(PAID, PARTIALLY_PAID, OVERPAID, EXPIRED);
    }

    /** Resolve enum by status code / 按状态编码解析枚举 */
    public static InvoiceStatusEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }

    /** Return immutable next-status options / 返回只读的允许迁移目标状态集合 */
    public Set<InvoiceStatusEnum> getAllowedTransitions() {
        return Collections.unmodifiableSet(allowedTransitions);
    }

    /** Check whether current status can move to target status / 判断当前状态是否可迁移到目标状态 */
    public boolean canTransitionTo(InvoiceStatusEnum targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        return this == targetStatus || allowedTransitions.contains(targetStatus);
    }

    /** Validate status transition and throw on illegal moves / 校验状态迁移是否合法，非法时抛出异常 */
    public static void ensureCanTransition(InvoiceStatusEnum fromStatus, InvoiceStatusEnum targetStatus) {
        if (fromStatus == null || targetStatus == null) {
            throw new IllegalArgumentException("Invoice status transition requires both source and target statuses");
        }
        if (!fromStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                "Illegal invoice status transition from " + fromStatus.getCode() + " to " + targetStatus.getCode()
            );
        }
    }
}
