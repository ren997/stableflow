package com.stableflow.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.dashboard.dto.DashboardSummaryQueryDto;
import com.stableflow.dashboard.service.DashboardService;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.system.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
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
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(dashboardService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldReturnDashboardSummaryViaPost() throws Exception {
        when(dashboardService.getSummary()).thenReturn(
            new DashboardSummaryVo(10L, 6L, 2L, 2L, new BigDecimal("99.50"))
        );

        mockMvc.perform(
                post("/api/dashboard/summary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new DashboardSummaryQueryDto()))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalInvoices").value(10))
            .andExpect(jsonPath("$.data.paidCount").value(6))
            .andExpect(jsonPath("$.data.totalReceivedAmount").value(99.50));
    }

    @Test
    void shouldNotExposeGetDashboardSummaryRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/summary"))
            .andExpect(status().isMethodNotAllowed());
    }
}
