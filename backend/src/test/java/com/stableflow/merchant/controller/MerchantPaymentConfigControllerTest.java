package com.stableflow.merchant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.merchant.dto.MerchantPaymentConfigQueryDto;
import com.stableflow.merchant.dto.MerchantPaymentConfigRequestDto;
import com.stableflow.merchant.service.MerchantPaymentConfigService;
import com.stableflow.merchant.vo.MerchantPaymentConfigVo;
import com.stableflow.system.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
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
class MerchantPaymentConfigControllerTest {

    @Mock
    private MerchantPaymentConfigService merchantPaymentConfigService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantPaymentConfigController(merchantPaymentConfigService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldSaveMerchantPaymentConfigViaPost() throws Exception {
        when(merchantPaymentConfigService.saveOrUpdate(any(MerchantPaymentConfigRequestDto.class))).thenReturn(configVo());

        mockMvc.perform(
                post("/api/merchant/payment-config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new MerchantPaymentConfigRequestDto("wallet-1", "mint-1", "SOLANA")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.walletAddress").value("wallet-1"))
            .andExpect(jsonPath("$.data.activeFlag").value(true));
    }

    @Test
    void shouldReturnCurrentConfigViaPost() throws Exception {
        when(merchantPaymentConfigService.getCurrentConfig()).thenReturn(configVo());

        mockMvc.perform(
                post("/api/merchant/payment-config/get")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new MerchantPaymentConfigQueryDto()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.walletAddress").value("wallet-1"))
            .andExpect(jsonPath("$.data.chain").value("SOLANA"));
    }

    @Test
    void shouldNotExposeGetCurrentConfigRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/merchant/payment-config"))
            .andExpect(status().isMethodNotAllowed());
    }

    private MerchantPaymentConfigVo configVo() {
        return new MerchantPaymentConfigVo(
            1L,
            10L,
            "wallet-1",
            "mint-1",
            "SOLANA",
            Boolean.TRUE,
            OffsetDateTime.parse("2026-03-20T10:00:00Z"),
            OffsetDateTime.parse("2026-03-20T10:00:00Z")
        );
    }
}
