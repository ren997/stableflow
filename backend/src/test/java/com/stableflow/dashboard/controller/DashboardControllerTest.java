package com.stableflow.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stableflow.dashboard.dto.DashboardExceptionInvoiceQueryDto;
import com.stableflow.dashboard.dto.DashboardSummaryQueryDto;
import com.stableflow.dashboard.service.DashboardService;
import com.stableflow.dashboard.vo.DashboardExceptionInvoiceVo;
import com.stableflow.dashboard.vo.DashboardInvoiceStatusDistributionVo;
import com.stableflow.dashboard.vo.DashboardSummaryVo;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.system.api.PageResult;
import com.stableflow.system.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
    void shouldAcceptDashboardSummaryPostWithoutBody() throws Exception {
        when(dashboardService.getSummary()).thenReturn(
            new DashboardSummaryVo(10L, 6L, 2L, 2L, new BigDecimal("99.50"))
        );

        mockMvc.perform(post("/api/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalInvoices").value(10));
    }

    @Test
    void shouldNotExposeGetDashboardSummaryRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/summary"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReturnInvoiceStatusDistributionViaPost() throws Exception {
        when(dashboardService.getInvoiceStatusDistribution()).thenReturn(
            new DashboardInvoiceStatusDistributionVo(
                List.of(
                    new DashboardInvoiceStatusDistributionVo.StatusCountItem(InvoiceStatusEnum.DRAFT, 1L),
                    new DashboardInvoiceStatusDistributionVo.StatusCountItem(InvoiceStatusEnum.PENDING, 2L),
                    new DashboardInvoiceStatusDistributionVo.StatusCountItem(InvoiceStatusEnum.PAID, 3L)
                )
            )
        );

        mockMvc.perform(post("/api/dashboard/invoices/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].status").value("DRAFT"))
            .andExpect(jsonPath("$.data.items[1].count").value(2))
            .andExpect(jsonPath("$.data.items[2].status").value("PAID"));
    }

    @Test
    void shouldNotExposeGetInvoiceStatusDistributionRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/invoices/status"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReturnExceptionInvoicesViaPost() throws Exception {
        when(dashboardService.getExceptionInvoices(ExceptionTagEnum.LATE_PAYMENT, 2, 10)).thenReturn(
            new PageResult<>(
                List.of(
                    new DashboardExceptionInvoiceVo(
                        1L,
                        "pub-001",
                        "INV-001",
                        "Alice",
                        new BigDecimal("99.00"),
                        "USDC",
                        InvoiceStatusEnum.EXPIRED,
                        List.of(ExceptionTagEnum.LATE_PAYMENT),
                        OffsetDateTime.parse("2026-03-21T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-20T10:00:00Z")
                    )
                ),
                1L,
                2L,
                10L
            )
        );

        mockMvc.perform(
                post("/api/dashboard/invoices/exceptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new DashboardExceptionInvoiceQueryDto(ExceptionTagEnum.LATE_PAYMENT, 2, 10)
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].invoiceNo").value("INV-001"))
            .andExpect(jsonPath("$.data.records[0].exceptionTags[0]").value("LATE_PAYMENT"))
            .andExpect(jsonPath("$.data.page").value(2));
    }

    @Test
    void shouldUseDefaultPaginationForExceptionInvoicesWhenBodyIsMissing() throws Exception {
        when(dashboardService.getExceptionInvoices(null, 1, 20)).thenReturn(
            new PageResult<>(List.of(), 0L, 1L, 20L)
        );

        mockMvc.perform(post("/api/dashboard/invoices/exceptions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.size").value(20));
    }
}
