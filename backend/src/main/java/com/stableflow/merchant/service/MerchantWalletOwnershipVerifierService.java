package com.stableflow.merchant.service;

import com.stableflow.merchant.vo.MerchantWalletOwnershipVerificationResultVo;

public interface MerchantWalletOwnershipVerifierService {

    /** Verify the submitted wallet signature against the challenge contract / 按挑战契约验证提交的钱包签名 */
    MerchantWalletOwnershipVerificationResultVo verify(
        String walletAddress,
        String chain,
        String challengeCode,
        String challengeMessage,
        String signature
    );
}
