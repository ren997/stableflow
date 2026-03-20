package com.stableflow.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.auth.dto.CurrentUserRequestDto;
import com.stableflow.auth.dto.RegisterRequestDto;
import com.stableflow.auth.service.AuthService;
import com.stableflow.auth.vo.CurrentUserVo;
import com.stableflow.auth.vo.LoginResponseVo;
import com.stableflow.merchant.enums.MerchantStatusEnum;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import com.stableflow.system.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldAcceptRegisterRequestWithoutAuthorizationHeader() throws Exception {
        when(authService.register(any(RegisterRequestDto.class))).thenReturn(
            new LoginResponseVo("jwt-token", "Bearer", 100L, "demo@stableflow.com", "StableFlow Demo")
        );

        mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new RegisterRequestDto("StableFlow Demo", "demo@stableflow.com", "Password123")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.data.merchantId").value(100L))
            .andExpect(jsonPath("$.data.email").value("demo@stableflow.com"))
            .andExpect(jsonPath("$.data.merchantName").value("StableFlow Demo"));
    }

    @Test
    void shouldRejectInvalidRegisterRequest() throws Exception {
        mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "merchantName": "",
                          "email": "invalid-email",
                          "password": "123"
                        }
                        """
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void shouldReturnBusinessErrorWhenRegisteringDuplicateEmail() throws Exception {
        when(authService.register(any(RegisterRequestDto.class)))
            .thenThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED));

        mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new RegisterRequestDto("StableFlow Demo", "demo@stableflow.com", "Password123")
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(40003))
            .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void shouldReturnCurrentMerchantInfo() throws Exception {
        when(authService.me()).thenReturn(
            new CurrentUserVo(100L, "StableFlow Demo", "demo@stableflow.com", MerchantStatusEnum.ACTIVE)
        );

        mockMvc.perform(
                post("/api/auth/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new CurrentUserRequestDto()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.merchantId").value(100L))
            .andExpect(jsonPath("$.data.merchantName").value("StableFlow Demo"))
            .andExpect(jsonPath("$.data.email").value("demo@stableflow.com"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldAcknowledgeLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).logout();
    }

    @Test
    void shouldNotExposeGetMeRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me"))
            .andExpect(status().isMethodNotAllowed());
    }
}
