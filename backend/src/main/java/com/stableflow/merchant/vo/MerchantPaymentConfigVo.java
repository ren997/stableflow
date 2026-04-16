package com.stableflow.merchant.vo;

import com.stableflow.merchant.enums.MerchantWalletOwnershipStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "MerchantPaymentConfigVo", description = "Merchant payment config response / 商家收款配置返回")
public record MerchantPaymentConfigVo(
    @Schema(description = "Config id / 配置 ID", example = "1")
    Long id,
    @Schema(description = "Merchant id / 商家 ID", example = "1")
    Long merchantId,
    @Schema(description = "Merchant fixed wallet address / 商家固定收款地址")
    String walletAddress,
    @Schema(description = "USDC mint address / USDC Mint 地址")
    String mintAddress,
    @Schema(description = "Blockchain name / 链名称", example = "SOLANA")
    String chain,
    @Schema(description = "Whether config is active / 是否启用")
    Boolean activeFlag,
    @Schema(description = MerchantWalletOwnershipStatusEnum.DESC, implementation = MerchantWalletOwnershipStatusEnum.class)
    MerchantWalletOwnershipStatusEnum ownershipVerificationStatus,
    @Schema(description = "Wallet ownership challenge expiry time in UTC / 钱包所有权挑战过期时间（UTC）")
    OffsetDateTime ownershipChallengeExpiresAt,
    @Schema(description = "Wallet ownership signature submitted time in UTC / 钱包所有权签名提交时间（UTC）")
    OffsetDateTime ownershipSignatureSubmittedAt,
    @Schema(description = "Wallet ownership verified time in UTC / 钱包所有权验证完成时间（UTC）")
    OffsetDateTime ownershipVerifiedAt,
    @Schema(description = "Created time in UTC / 创建时间（UTC）")
    OffsetDateTime createdAt,
    @Schema(description = "Updated time in UTC / 更新时间（UTC）")
    OffsetDateTime updatedAt
) {
}
