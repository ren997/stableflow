package com.stableflow.merchant.vo;

import com.stableflow.merchant.enums.MerchantWalletOwnershipStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(
    name = "MerchantWalletOwnershipChallengeVo",
    description = "Wallet ownership challenge response / 钱包地址所有权挑战返回"
)
public record MerchantWalletOwnershipChallengeVo(
    @Schema(description = "Config id / 配置 ID", example = "1")
    Long configId,
    @Schema(description = "Merchant fixed wallet address / 商家固定收款地址")
    String walletAddress,
    @Schema(description = "Blockchain name / 链名称", example = "SOLANA")
    String chain,
    @Schema(description = MerchantWalletOwnershipStatusEnum.DESC, implementation = MerchantWalletOwnershipStatusEnum.class)
    MerchantWalletOwnershipStatusEnum ownershipVerificationStatus,
    @Schema(description = "Generated challenge code / 生成的挑战码")
    String challengeCode,
    @Schema(description = "Human-readable challenge message to be signed / 需要签名的人类可读挑战消息")
    String challengeMessage,
    @Schema(description = "Challenge expiry time in UTC / 挑战过期时间（UTC）")
    OffsetDateTime challengeExpiresAt
) {
}
