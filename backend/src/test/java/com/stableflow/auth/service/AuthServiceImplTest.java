package com.stableflow.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(merchantMapper, passwordEncoder, jwtTokenProvider, currentMerchantProvider);
    }

    @Test
    void shouldRegisterMerchantAndReturnLoginResponse() {
        RegisterRequestDto request = new RegisterRequestDto("StableFlow Demo", " Demo@StableFlow.com ", "Password123");

        when(merchantMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<Merchant>>any())).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(jwtTokenProvider.generateToken(any(CurrentMerchant.class))).thenReturn("jwt-token");
        when(merchantMapper.insert(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant merchant = invocation.getArgument(0);
            merchant.setId(100L);
            return 1;
        });

        LoginResponseVo response = authService.register(request);

        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).insert(merchantCaptor.capture());
        Merchant savedMerchant = merchantCaptor.getValue();

        assertEquals("StableFlow Demo", savedMerchant.getMerchantName());
        assertEquals("demo@stableflow.com", savedMerchant.getEmail());
        assertEquals(MerchantStatusEnum.ACTIVE, savedMerchant.getStatus());
        assertEquals("hashed-password", savedMerchant.getPasswordHash());
        assertNotEquals("Password123", savedMerchant.getPasswordHash());

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(100L, response.merchantId());
        assertEquals("demo@stableflow.com", response.email());
        assertEquals("StableFlow Demo", response.merchantName());
    }

    @Test
    void shouldRejectDuplicateEmailWhenRegistering() {
        RegisterRequestDto request = new RegisterRequestDto("StableFlow Demo", "demo@stableflow.com", "Password123");
        Merchant existingMerchant = new Merchant();
        existingMerchant.setId(1L);
        existingMerchant.setEmail("demo@stableflow.com");

        when(merchantMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<Merchant>>any())).thenReturn(existingMerchant);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals(ErrorCode.EMAIL_ALREADY_REGISTERED, exception.getErrorCode());
    }

    @Test
    void shouldRejectDuplicateEmailAfterNormalization() {
        RegisterRequestDto request = new RegisterRequestDto("StableFlow Demo", " Demo@StableFlow.com ", "Password123");
        Merchant existingMerchant = new Merchant();
        existingMerchant.setId(1L);
        existingMerchant.setEmail("demo@stableflow.com");

        when(merchantMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<Merchant>>any())).thenReturn(existingMerchant);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));

        assertEquals(ErrorCode.EMAIL_ALREADY_REGISTERED, exception.getErrorCode());
    }

    @Test
    void shouldNormalizeEmailWhenLoggingIn() {
        LoginRequestDto request = new LoginRequestDto(" Demo@StableFlow.com ", "Password123");
        Merchant merchant = new Merchant();
        merchant.setId(12L);
        merchant.setEmail("demo@stableflow.com");
        merchant.setMerchantName("StableFlow Demo");
        merchant.setPasswordHash("hashed-password");

        when(merchantMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<Merchant>>any())).thenReturn(merchant);
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(CurrentMerchant.class))).thenReturn("jwt-token");

        LoginResponseVo response = authService.login(request);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("demo@stableflow.com", response.email());
        assertEquals("StableFlow Demo", response.merchantName());
    }

    @Test
    void shouldReturnCurrentMerchantProfile() {
        CurrentMerchant currentMerchant = new CurrentMerchant(12L, "demo@stableflow.com");
        Merchant merchant = new Merchant();
        merchant.setId(12L);
        merchant.setMerchantName("StableFlow Demo");
        merchant.setEmail("demo@stableflow.com");
        merchant.setStatus(MerchantStatusEnum.ACTIVE);

        when(currentMerchantProvider.requireCurrentMerchant()).thenReturn(currentMerchant);
        when(merchantMapper.selectById(12L)).thenReturn(merchant);

        CurrentUserVo response = authService.me();

        assertEquals(12L, response.merchantId());
        assertEquals("StableFlow Demo", response.merchantName());
        assertEquals("demo@stableflow.com", response.email());
        assertEquals(MerchantStatusEnum.ACTIVE, response.status());
    }
}
