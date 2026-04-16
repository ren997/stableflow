package com.stableflow.merchant.service;

import com.stableflow.merchant.enums.MerchantWalletOwnershipStatusEnum;
import com.stableflow.merchant.vo.MerchantWalletOwnershipVerificationResultVo;
import org.springframework.stereotype.Service;

@Service
public class MerchantWalletOwnershipVerifierServiceImpl implements MerchantWalletOwnershipVerifierService {

    @Override
    public MerchantWalletOwnershipVerificationResultVo verify(
        String walletAddress,
        String chain,
        String challengeCode,
        String challengeMessage,
        String signature
    ) {
        // 当前 MVP 先把挑战码、签名提交和状态流转收口成稳定契约，
        // 后续接入 Solana 钱包签名验签时只需要替换这里的实现。
        return new MerchantWalletOwnershipVerificationResultVo(
            MerchantWalletOwnershipStatusEnum.SIGNATURE_SUBMITTED,
            Boolean.FALSE,
            "Wallet signature stored. Cryptographic verification can be enabled by replacing the verifier implementation later."
        );
    }
}
