package com.stableflow.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.auth.dto.LoginRequestDto;
import com.stableflow.auth.vo.CurrentUserVo;
import com.stableflow.auth.vo.LoginResponseVo;
import com.stableflow.merchant.entity.Merchant;
import com.stableflow.merchant.mapper.MerchantMapper;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchant;
import com.stableflow.system.security.CurrentMerchantProvider;
import com.stableflow.system.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CurrentMerchantProvider currentMerchantProvider;

    public AuthServiceImpl(
        MerchantMapper merchantMapper,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        CurrentMerchantProvider currentMerchantProvider
    ) {
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.currentMerchantProvider = currentMerchantProvider;
    }

    @Override
    public LoginResponseVo login(LoginRequestDto request) {
        Merchant merchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>().eq(Merchant::getEmail, request.email())
        );
        if (merchant == null || !passwordEncoder.matches(request.password(), merchant.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        CurrentMerchant currentMerchant = new CurrentMerchant(merchant.getId(), merchant.getEmail());
        return new LoginResponseVo(
            jwtTokenProvider.generateToken(currentMerchant),
            "Bearer",
            merchant.getId(),
            merchant.getEmail(),
            merchant.getMerchantName()
        );
    }

    @Override
    public CurrentUserVo me() {
        CurrentMerchant currentMerchant = currentMerchantProvider.requireCurrentMerchant();
        return new CurrentUserVo(currentMerchant.merchantId(), currentMerchant.email());
    }
}
