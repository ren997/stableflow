package com.stableflow.merchant.vo;

import com.stableflow.merchant.enums.MerchantWalletOwnershipStatusEnum;

/** Wallet ownership verifier result for the current or future verifier implementation / 当前或后续钱包地址所有权验签实现的返回结果 */
public record MerchantWalletOwnershipVerificationResultVo(
    MerchantWalletOwnershipStatusEnum ownershipVerificationStatus,
    Boolean verifierReady,
    String verificationMessage
) {
}
