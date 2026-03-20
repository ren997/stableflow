package com.stableflow.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.auth.dto.LoginRequestDto;
import com.stableflow.auth.dto.RegisterRequestDto;
import com.stableflow.auth.vo.CurrentUserVo;
import com.stableflow.auth.vo.LoginResponseVo;
import com.stableflow.merchant.entity.Merchant;
import com.stableflow.merchant.enums.MerchantStatusEnum;
import com.stableflow.merchant.mapper.MerchantMapper;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.security.CurrentMerchant;
import com.stableflow.system.security.CurrentMerchantProvider;
import com.stableflow.system.security.JwtTokenProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CurrentMerchantProvider currentMerchantProvider;

    @Override
    public LoginResponseVo register(RegisterRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        Merchant existingMerchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>().eq(Merchant::getEmail, normalizedEmail)
        );
        if (existingMerchant != null) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        Merchant merchant = new Merchant();
        merchant.setMerchantName(request.merchantName().trim());
        merchant.setEmail(normalizedEmail);
        merchant.setPasswordHash(passwordEncoder.encode(request.password()));
        merchant.setStatus(MerchantStatusEnum.ACTIVE);
        merchantMapper.insert(merchant);

        return buildLoginResponse(merchant);
    }

    @Override
    public LoginResponseVo login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        Merchant merchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>().eq(Merchant::getEmail, normalizedEmail)
        );
        if (merchant == null || !passwordEncoder.matches(request.password(), merchant.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return buildLoginResponse(merchant);
    }

    @Override
    public void logout() {
        currentMerchantProvider.requireCurrentMerchant();
    }

    @Override
    public CurrentUserVo me() {
        CurrentMerchant currentMerchant = currentMerchantProvider.requireCurrentMerchant();
        Merchant merchant = merchantMapper.selectById(currentMerchant.merchantId());
        if (merchant == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        return new CurrentUserVo(
            merchant.getId(),
            merchant.getMerchantName(),
            merchant.getEmail(),
            merchant.getStatus()
        );
    }

    private LoginResponseVo buildLoginResponse(Merchant merchant) {
        CurrentMerchant currentMerchant = new CurrentMerchant(merchant.getId(), merchant.getEmail());
        return new LoginResponseVo(
            jwtTokenProvider.generateToken(currentMerchant),
            "Bearer",
            merchant.getId(),
            merchant.getEmail(),
            merchant.getMerchantName()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
