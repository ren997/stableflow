package com.stableflow.merchant.vo;

import com.stableflow.merchant.enums.MerchantWalletOwnershipStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(
    name = "MerchantWalletOwnershipVerifyVo",
    description = "Wallet ownership verification submit response / 钱包地址所有权验证提交返回"
)
public record MerchantWalletOwnershipVerifyVo(
    @Schema(description = "Config id / 配置 ID", example = "1")
    Long configId,
    @Schema(description = "Merchant fixed wallet address / 商家固定收款地址")
    String walletAddress,
    @Schema(description = MerchantWalletOwnershipStatusEnum.DESC, implementation = MerchantWalletOwnershipStatusEnum.class)
    MerchantWalletOwnershipStatusEnum ownershipVerificationStatus,
    @Schema(description = "Whether the actual cryptographic verifier is enabled / 当前是否已启用真实密码学验签")
    Boolean verifierReady,
    @Schema(description = "Verification result message / 验签结果说明")
    String verificationMessage,
    @Schema(description = "Challenge expiry time in UTC / 挑战过期时间（UTC）")
    OffsetDateTime challengeExpiresAt,
    @Schema(description = "Signature submitted time in UTC / 签名提交时间（UTC）")
    OffsetDateTime signatureSubmittedAt,
    @Schema(description = "Wallet ownership verified time in UTC / 钱包所有权验证完成时间（UTC）")
    OffsetDateTime verifiedAt
) {
}
