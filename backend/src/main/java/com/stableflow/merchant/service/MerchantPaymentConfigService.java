package com.stableflow.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.entity.MerchantPaymentConfig;
import com.stableflow.merchant.mapper.MerchantPaymentConfigMapper;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchantProvider;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantPaymentConfigService {

    private final MerchantPaymentConfigMapper paymentConfigMapper;
    private final CurrentMerchantProvider currentMerchantProvider;

    public MerchantPaymentConfigService(
        MerchantPaymentConfigMapper paymentConfigMapper,
        CurrentMerchantProvider currentMerchantProvider
    ) {
        this.paymentConfigMapper = paymentConfigMapper;
        this.currentMerchantProvider = currentMerchantProvider;
    }

    @Transactional
    public MerchantPaymentConfigVo saveOrUpdate(MerchantPaymentConfigRequestDto request) {
        Long merchantId = currentMerchantProvider.requireCurrentMerchantId();
        MerchantPaymentConfig config = paymentConfigMapper.selectOne(
            new LambdaQueryWrapper<MerchantPaymentConfig>().eq(MerchantPaymentConfig::getMerchantId, merchantId)
        );
        if (config == null) {
            config = new MerchantPaymentConfig();
            config.setMerchantId(merchantId);
        }
        config.setWalletAddress(request.walletAddress());
        config.setMintAddress(request.mintAddress());
        config.setChain(request.chain());
        config.setActiveFlag(Boolean.TRUE);
        if (config.getId() == null) {
            paymentConfigMapper.insert(config);
        } else {
            paymentConfigMapper.updateById(config);
        }
        return toResponse(config);
    }

    public MerchantPaymentConfigVo getCurrentConfig() {
        MerchantPaymentConfig config = getRequiredConfig(currentMerchantProvider.requireCurrentMerchantId());
        return toResponse(config);
    }

    public MerchantPaymentConfig getRequiredConfig(Long merchantId) {
        MerchantPaymentConfig config = paymentConfigMapper.selectOne(
            new LambdaQueryWrapper<MerchantPaymentConfig>()
                .eq(MerchantPaymentConfig::getMerchantId, merchantId)
                .eq(MerchantPaymentConfig::getActiveFlag, Boolean.TRUE)
        );
        if (config == null) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIG_NOT_FOUND);
        }
        return config;
    }

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
            config.getCreatedAt(),
            config.getUpdatedAt()
        );
    }
}
