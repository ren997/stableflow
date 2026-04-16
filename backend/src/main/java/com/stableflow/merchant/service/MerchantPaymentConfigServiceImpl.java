package com.stableflow.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.dto.MerchantWalletOwnershipVerifyRequestDto;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.merchant.enums.MerchantWalletOwnershipStatusEnum;
import com.stableflow.merchant.mapper.MerchantPaymentConfigMapper;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import com.stableflow.merchant.vo.MerchantWalletOwnershipChallengeVo;
import com.stableflow.merchant.vo.MerchantWalletOwnershipVerificationResultVo;
import com.stableflow.merchant.vo.MerchantWalletOwnershipVerifyVo;
import com.stableflow.system.config.MerchantOwnershipProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantPaymentConfigServiceImpl
    extends ServiceImpl<MerchantPaymentConfigMapper, MerchantPaymentConfig>
    implements MerchantPaymentConfigService {

    private final MerchantPaymentConfigMapper paymentConfigMapper;
    private final CurrentMerchantProvider currentMerchantProvider;
    private final MerchantOwnershipProperties merchantOwnershipProperties;
    private final MerchantWalletOwnershipVerifierService merchantWalletOwnershipVerifierService;

    @Transactional
    @Override
    public MerchantPaymentConfigVo saveOrUpdate(MerchantPaymentConfigRequestDto request) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        MerchantPaymentConfig config = paymentConfigMapper.selectOne(
            new LambdaQueryWrapper<MerchantPaymentConfig>().eq(MerchantPaymentConfig::getMerchantId, merchantId)
        );
        if (config == null) {
            config = new MerchantPaymentConfig();
            config.setMerchantId(merchantId);
        }
        boolean ownershipTargetChanged = isOwnershipTargetChanged(config, request);
        config.setWalletAddress(request.walletAddress());
        config.setMintAddress(request.mintAddress());
        config.setChain(request.chain());
        config.setActiveFlag(Boolean.TRUE);
        if (ownershipTargetChanged) {
            resetOwnershipState(config);
        } else if (config.getOwnershipVerificationStatus() == null) {
            config.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.UNVERIFIED);
        }
        if (config.getId() == null) {
            paymentConfigMapper.insert(config);
        } else {
            paymentConfigMapper.updateById(config);
        }
        return toResponse(config);
    }

    @Override
    public MerchantPaymentConfigVo getCurrentConfig() {
        MerchantPaymentConfig config = getRequiredConfig(currentMerchantProvider.requireCurrentMerchantId());
        return toResponse(config);
    }

    @Transactional
    @Override
    public MerchantWalletOwnershipChallengeVo createOwnershipChallenge() {
        MerchantPaymentConfig config = getRequiredConfig(currentMerchantProvider.requireCurrentMerchantId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plus(merchantOwnershipProperties.challengeTtl());
        String challengeCode = "own_" + UUID.randomUUID().toString().replace("-", "");
        String challengeMessage = buildChallengeMessage(config, challengeCode, now, expiresAt);

        config.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.CHALLENGE_ISSUED);
        config.setOwnershipChallengeCode(challengeCode);
        config.setOwnershipChallengeMessage(challengeMessage);
        config.setOwnershipChallengeExpiresAt(expiresAt);
        config.setOwnershipVerificationSignature(null);
        config.setOwnershipSignatureSubmittedAt(null);
        config.setOwnershipVerifiedAt(null);
        paymentConfigMapper.updateById(config);

        return new MerchantWalletOwnershipChallengeVo(
            config.getId(),
            config.getWalletAddress(),
            config.getChain(),
            config.getOwnershipVerificationStatus(),
            config.getOwnershipChallengeCode(),
            config.getOwnershipChallengeMessage(),
            config.getOwnershipChallengeExpiresAt()
        );
    }

    @Transactional
    @Override
    public MerchantWalletOwnershipVerifyVo verifyOwnership(MerchantWalletOwnershipVerifyRequestDto request) {
        MerchantPaymentConfig config = getRequiredConfig(currentMerchantProvider.requireCurrentMerchantId());
        validateChallenge(config, request.challengeCode());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        config.setOwnershipVerificationSignature(request.signature());
        config.setOwnershipSignatureSubmittedAt(now);

        MerchantWalletOwnershipVerificationResultVo verificationResult =
            merchantWalletOwnershipVerifierService.verify(
                config.getWalletAddress(),
                config.getChain(),
                request.challengeCode(),
                config.getOwnershipChallengeMessage(),
                request.signature()
            );
        config.setOwnershipVerificationStatus(verificationResult.ownershipVerificationStatus());
        if (MerchantWalletOwnershipStatusEnum.VERIFIED.equals(verificationResult.ownershipVerificationStatus())) {
            config.setOwnershipVerifiedAt(now);
        } else if (!MerchantWalletOwnershipStatusEnum.CHALLENGE_ISSUED.equals(verificationResult.ownershipVerificationStatus())) {
            config.setOwnershipVerifiedAt(null);
        }
        paymentConfigMapper.updateById(config);

        return new MerchantWalletOwnershipVerifyVo(
            config.getId(),
            config.getWalletAddress(),
            config.getOwnershipVerificationStatus(),
            verificationResult.verifierReady(),
            verificationResult.verificationMessage(),
            config.getOwnershipChallengeExpiresAt(),
            config.getOwnershipSignatureSubmittedAt(),
            config.getOwnershipVerifiedAt()
        );
    }

    @Override
    public MerchantPaymentConfig getRequiredConfig(Long merchantId) {
        MerchantPaymentConfig config = paymentConfigMapper.selectOne(
            new LambdaQueryWrapper<MerchantPaymentConfig>()
                .eq(MerchantPaymentConfig::getMerchantId, merchantId)
                .eq(MerchantPaymentConfig::getActiveFlag, Boolean.TRUE)
        );
        if (config == null) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIG_NOT_FOUND);
        }
        if (config.getOwnershipVerificationStatus() == null) {
            config.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.UNVERIFIED);
        }
        return config;
    }

    @Override
    public List<MerchantPaymentConfig> listActiveConfigs() {
        return paymentConfigMapper.selectList(
            new LambdaQueryWrapper<MerchantPaymentConfig>()
                .eq(MerchantPaymentConfig::getActiveFlag, Boolean.TRUE)
        );
    }

    private MerchantPaymentConfigVo toResponse(MerchantPaymentConfig config) {
        return new MerchantPaymentConfigVo(
            config.getId(),
            config.getMerchantId(),
            config.getWalletAddress(),
            config.getMintAddress(),
            config.getChain(),
            config.getActiveFlag(),
            config.getOwnershipVerificationStatus(),
            config.getOwnershipChallengeExpiresAt(),
            config.getOwnershipSignatureSubmittedAt(),
            config.getOwnershipVerifiedAt(),
            config.getCreatedAt(),
            config.getUpdatedAt()
        );
    }

    private boolean isOwnershipTargetChanged(MerchantPaymentConfig config, MerchantPaymentConfigRequestDto request) {
        return config.getId() == null
            || !Objects.equals(config.getWalletAddress(), request.walletAddress())
            || !Objects.equals(config.getChain(), request.chain());
    }

    private void resetOwnershipState(MerchantPaymentConfig config) {
        config.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.UNVERIFIED);
        config.setOwnershipChallengeCode(null);
        config.setOwnershipChallengeMessage(null);
        config.setOwnershipChallengeExpiresAt(null);
        config.setOwnershipVerificationSignature(null);
        config.setOwnershipSignatureSubmittedAt(null);
        config.setOwnershipVerifiedAt(null);
    }

    private String buildChallengeMessage(
        MerchantPaymentConfig config,
        String challengeCode,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt
    ) {
        return String.join(
            "\n",
            "StableFlow Wallet Ownership Verification",
            "merchantId: " + config.getMerchantId(),
            "walletAddress: " + config.getWalletAddress(),
            "chain: " + config.getChain(),
            "challengeCode: " + challengeCode,
            "issuedAt: " + issuedAt,
            "expiresAt: " + expiresAt
        );
    }

    private void validateChallenge(MerchantPaymentConfig config, String challengeCode) {
        if (config.getOwnershipChallengeCode() == null || config.getOwnershipChallengeMessage() == null) {
            throw new BusinessException(
                ErrorCode.WALLET_OWNERSHIP_CHALLENGE_NOT_FOUND,
                "Wallet ownership challenge not found, please request a new challenge first."
            );
        }
        if (!Objects.equals(config.getOwnershipChallengeCode(), challengeCode)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Wallet ownership challenge code does not match.");
        }
        if (config.getOwnershipChallengeExpiresAt() == null) {
            throw new BusinessException(
                ErrorCode.WALLET_OWNERSHIP_CHALLENGE_NOT_FOUND,
                "Wallet ownership challenge expiry is missing, please request a new challenge first."
            );
        }
        if (config.getOwnershipChallengeExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(
                ErrorCode.WALLET_OWNERSHIP_CHALLENGE_EXPIRED,
                "Wallet ownership challenge expired, please request a new challenge."
            );
        }
    }
}
