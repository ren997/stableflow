package com.stableflow.merchant.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Merchant account status enum / 商家账户状态枚举 */
@Getter
public enum MerchantStatusEnum {
    /** Merchant account is active and can access the system / 商家账户已启用，可正常访问系统 */
    ACTIVE("ACTIVE", "启用");

    public static final String DESC = "商家状态: ACTIVE-启用";

    private static final Map<String, MerchantStatusEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(MerchantStatusEnum::getCode, Function.identity()));

    /** Persisted merchant status code / 持久化商家状态编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable merchant status description / 可读商家状态说明 */
    private final String desc;

    MerchantStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by merchant status code / 按商家状态编码解析枚举 */
    public static MerchantStatusEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
