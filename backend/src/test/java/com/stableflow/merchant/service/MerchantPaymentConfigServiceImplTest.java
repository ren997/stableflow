package com.stableflow.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.stableflow.system.security.CurrentMerchantProvider;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantPaymentConfigServiceImplTest {

    @Mock
    private MerchantPaymentConfigMapper paymentConfigMapper;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    @Mock
    private MerchantWalletOwnershipVerifierService merchantWalletOwnershipVerifierService;

    private MerchantPaymentConfigServiceImpl merchantPaymentConfigService;

    @BeforeEach
    void setUp() {
        merchantPaymentConfigService = new MerchantPaymentConfigServiceImpl(
            paymentConfigMapper,
            currentMerchantProvider,
            new MerchantOwnershipProperties(Duration.ofMinutes(10)),
            merchantWalletOwnershipVerifierService
        );
    }

    @Test
    void shouldResetOwnershipStateWhenWalletChanges() {
        MerchantPaymentConfig existingConfig = baseConfig();
        existingConfig.setWalletAddress("wallet-old");
        existingConfig.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.VERIFIED);
        existingConfig.setOwnershipChallengeCode("old_challenge");
        existingConfig.setOwnershipChallengeMessage("old message");
        existingConfig.setOwnershipChallengeExpiresAt(OffsetDateTime.parse("2026-03-20T10:10:00Z"));
        existingConfig.setOwnershipVerificationSignature("old_signature");
        existingConfig.setOwnershipSignatureSubmittedAt(OffsetDateTime.parse("2026-03-20T10:05:00Z"));
        existingConfig.setOwnershipVerifiedAt(OffsetDateTime.parse("2026-03-20T10:06:00Z"));

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(paymentConfigMapper.selectOne(any())).thenReturn(existingConfig);

        MerchantPaymentConfigVo result = merchantPaymentConfigService.saveOrUpdate(
            new MerchantPaymentConfigRequestDto("wallet-new", "mint-1", "SOLANA")
        );

        assertEquals(MerchantWalletOwnershipStatusEnum.UNVERIFIED, result.ownershipVerificationStatus());
        assertNull(result.ownershipChallengeExpiresAt());
        assertNull(result.ownershipSignatureSubmittedAt());
        assertNull(result.ownershipVerifiedAt());
        verify(paymentConfigMapper).updateById(existingConfig);
    }

    @Test
    void shouldCreateOwnershipChallengeForCurrentConfig() {
        MerchantPaymentConfig config = baseConfig();

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(paymentConfigMapper.selectOne(any())).thenReturn(config);

        MerchantWalletOwnershipChallengeVo result = merchantPaymentConfigService.createOwnershipChallenge();

        assertEquals(MerchantWalletOwnershipStatusEnum.CHALLENGE_ISSUED, result.ownershipVerificationStatus());
        assertEquals("wallet-1", result.walletAddress());
        assertEquals("SOLANA", result.chain());
        assertEquals(config.getOwnershipChallengeCode(), result.challengeCode());
        assertEquals(config.getOwnershipChallengeMessage(), result.challengeMessage());
        verify(paymentConfigMapper).updateById(config);
    }

    @Test
    void shouldStoreSubmittedSignatureAndKeepReservedStatusWhenVerifierNotReady() {
        MerchantPaymentConfig config = baseConfig();
        config.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.CHALLENGE_ISSUED);
        config.setOwnershipChallengeCode("own_abc");
        config.setOwnershipChallengeMessage("sign this challenge");
        config.setOwnershipChallengeExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(paymentConfigMapper.selectOne(any())).thenReturn(config);
        when(merchantWalletOwnershipVerifierService.verify(
            "wallet-1",
            "SOLANA",
            "own_abc",
            "sign this challenge",
            "signed_payload"
        )).thenReturn(
            new MerchantWalletOwnershipVerificationResultVo(
                MerchantWalletOwnershipStatusEnum.SIGNATURE_SUBMITTED,
                Boolean.FALSE,
                "Wallet signature stored."
            )
        );

        MerchantWalletOwnershipVerifyVo result = merchantPaymentConfigService.verifyOwnership(
            new MerchantWalletOwnershipVerifyRequestDto("own_abc", "signed_payload")
        );

        assertEquals(MerchantWalletOwnershipStatusEnum.SIGNATURE_SUBMITTED, result.ownershipVerificationStatus());
        assertEquals(Boolean.FALSE, result.verifierReady());
        assertEquals("Wallet signature stored.", result.verificationMessage());
        assertEquals("signed_payload", config.getOwnershipVerificationSignature());
        verify(paymentConfigMapper).updateById(config);
    }

    @Test
    void shouldRejectOwnershipVerificationWhenChallengeDoesNotMatch() {
        MerchantPaymentConfig config = baseConfig();
        config.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.CHALLENGE_ISSUED);
        config.setOwnershipChallengeCode("own_abc");
        config.setOwnershipChallengeMessage("sign this challenge");
        config.setOwnershipChallengeExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(paymentConfigMapper.selectOne(any())).thenReturn(config);

        assertThrows(
            BusinessException.class,
            () -> merchantPaymentConfigService.verifyOwnership(
                new MerchantWalletOwnershipVerifyRequestDto("own_other", "signed_payload")
            )
        );
        verify(merchantWalletOwnershipVerifierService, never()).verify(any(), any(), any(), any(), any());
    }

    private MerchantPaymentConfig baseConfig() {
        MerchantPaymentConfig config = new MerchantPaymentConfig();
        config.setId(1L);
        config.setMerchantId(10L);
        config.setWalletAddress("wallet-1");
        config.setMintAddress("mint-1");
        config.setChain("SOLANA");
        config.setActiveFlag(Boolean.TRUE);
        config.setOwnershipVerificationStatus(MerchantWalletOwnershipStatusEnum.UNVERIFIED);
        return config;
    }
}
