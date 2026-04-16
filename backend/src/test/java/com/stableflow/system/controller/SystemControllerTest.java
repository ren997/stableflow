package com.stableflow.system.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stableflow.system.enums.SolanaNetworkEnum;
import com.stableflow.system.exception.GlobalExceptionHandler;
import com.stableflow.system.service.SystemRuntimeConfigService;
import com.stableflow.system.vo.SystemRuntimeConfigVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SystemControllerTest {

    @Mock
    private SystemRuntimeConfigService systemRuntimeConfigService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SystemController(systemRuntimeConfigService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnRuntimeConfigViaPost() throws Exception {
        when(systemRuntimeConfigService.getRuntimeConfig()).thenReturn(
            new SystemRuntimeConfigVo(
                SolanaNetworkEnum.DEVNET,
                "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
                "https://explorer.solana.com/tx/"
            )
        );

        mockMvc.perform(post("/api/system/runtime-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.solanaNetwork").value("DEVNET"))
            .andExpect(jsonPath("$.data.defaultMintAddress").value("4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"))
            .andExpect(jsonPath("$.data.explorerTxBaseUrl").value("https://explorer.solana.com/tx/"));
    }
}
