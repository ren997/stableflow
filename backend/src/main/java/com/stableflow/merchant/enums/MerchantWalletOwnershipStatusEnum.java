package com.stableflow.merchant.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

/** Merchant wallet ownership verification status enum / 商家钱包地址所有权验证状态枚举 */
@Getter
public enum MerchantWalletOwnershipStatusEnum {
    /** Wallet ownership has not entered any verification flow yet / 钱包地址所有权尚未进入验证流程 */
    UNVERIFIED("UNVERIFIED", "未验证"),
    /** Challenge has been issued and waits for wallet signature / 已生成挑战码，等待钱包签名 */
    CHALLENGE_ISSUED("CHALLENGE_ISSUED", "挑战已下发"),
    /** Signature has been submitted and stored for current or future verification / 已提交签名，等待当前或后续验签处理 */
    SIGNATURE_SUBMITTED("SIGNATURE_SUBMITTED", "签名已提交"),
    /** Wallet ownership has been verified successfully / 钱包地址所有权已验证成功 */
    VERIFIED("VERIFIED", "已验证"),
    /** Wallet ownership verification failed / 钱包地址所有权验证失败 */
    FAILED("FAILED", "验证失败");

    public static final String DESC =
        "钱包地址所有权验证状态: UNVERIFIED-未验证, CHALLENGE_ISSUED-挑战已下发, SIGNATURE_SUBMITTED-签名已提交, VERIFIED-已验证, FAILED-验证失败";

    private static final Map<String, MerchantWalletOwnershipStatusEnum> CODE_MAP = Stream.of(values())
        .collect(Collectors.toMap(MerchantWalletOwnershipStatusEnum::getCode, Function.identity()));

    /** Persisted wallet ownership verification status code / 持久化钱包地址所有权验证状态编码 */
    @EnumValue
    @JsonValue
    private final String code;

    /** Human-readable wallet ownership verification status description / 可读钱包地址所有权验证状态说明 */
    private final String desc;

    MerchantWalletOwnershipStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** Resolve enum by wallet ownership verification status code / 按钱包地址所有权验证状态编码解析枚举 */
    public static MerchantWalletOwnershipStatusEnum fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
