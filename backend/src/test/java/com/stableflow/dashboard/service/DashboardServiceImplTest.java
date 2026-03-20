package com.stableflow.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import com.stableflow.dashboard.vo.DashboardExceptionInvoiceVo;
import com.stableflow.dashboard.vo.DashboardInvoiceStatusDistributionVo;
import com.stableflow.dashboard.enums.DashboardTimeGranularityEnum;
import com.stableflow.dashboard.vo.DashboardSummaryTrendVo;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.system.api.PageResult;
import com.stableflow.system.security.CurrentMerchantProvider;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private PaymentTransactionMapper paymentTransactionMapper;

    @Mock
    private CurrentMerchantProvider currentMerchantProvider;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(invoiceService, paymentTransactionMapper, currentMerchantProvider);
    }

    @Test
    void shouldReturnAllInvoiceStatusesInDeclarationOrder() {
        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceService.count(org.mockito.ArgumentMatchers.any())).thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L);

        DashboardInvoiceStatusDistributionVo response = dashboardService.getInvoiceStatusDistribution();

        assertEquals(InvoiceStatusEnum.values().length, response.items().size());
        assertEquals(InvoiceStatusEnum.DRAFT, response.items().get(0).status());
        assertEquals(1L, response.items().get(0).count());
        assertEquals(InvoiceStatusEnum.FAILED_RECONCILIATION, response.items().get(6).status());
        assertEquals(7L, response.items().get(6).count());
        verify(invoiceService, times(InvoiceStatusEnum.values().length)).count(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnZeroCountStatusesWithoutDroppingThem() {
        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceService.count(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        DashboardInvoiceStatusDistributionVo response = dashboardService.getInvoiceStatusDistribution();

        assertEquals(InvoiceStatusEnum.values().length, response.items().size());
        response.items().forEach(item -> assertEquals(0L, item.count()));
    }

    @Test
    void shouldReturnDailySummaryTrendByDefault() {
        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(paymentTransactionMapper.listDailyVerifiedTrendByMerchantId(10L)).thenReturn(
            List.of(
                Map.of(
                    "bucketstartat", Timestamp.from(OffsetDateTime.parse("2026-03-20T00:00:00Z").toInstant()),
                    "totalreceivedamount", new BigDecimal("99.50"),
                    "transactioncount", 2L
                )
            )
        );

        DashboardSummaryTrendVo response = dashboardService.getSummaryTrend(null);

        assertEquals(DashboardTimeGranularityEnum.DAY, response.granularity());
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("99.50"), response.items().get(0).totalReceivedAmount());
        assertEquals(2L, response.items().get(0).transactionCount());
    }

    @Test
    void shouldDispatchSummaryTrendByRequestedGranularity() {
        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(paymentTransactionMapper.listMonthlyVerifiedTrendByMerchantId(10L)).thenReturn(List.of());

        DashboardSummaryTrendVo response = dashboardService.getSummaryTrend(DashboardTimeGranularityEnum.MONTH);

        assertEquals(DashboardTimeGranularityEnum.MONTH, response.granularity());
        verify(paymentTransactionMapper).listMonthlyVerifiedTrendByMerchantId(10L);
    }

    @Test
    void shouldReturnExceptionInvoicesWithNormalizedTags() {
        when(currentMerchantProvider.requireCurrentMerchantId()).thenReturn(10L);
        when(invoiceService.page(any(Page.class), any())).thenReturn(exceptionInvoicePage());

        PageResult<DashboardExceptionInvoiceVo> response = dashboardService.getExceptionInvoices(ExceptionTagEnum.LATE_PAYMENT, 2, 10);

        assertEquals(1L, response.total());
        assertEquals(2L, response.page());
        assertEquals(InvoiceStatusEnum.EXPIRED, response.records().get(0).status());
        assertEquals(List.of(ExceptionTagEnum.LATE_PAYMENT), response.records().get(0).exceptionTags());
        verify(invoiceService).page(any(Page.class), any());
    }

    private Page<Invoice> exceptionInvoicePage() {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setPublicId("pub-001");
        invoice.setInvoiceNo("INV-001");
        invoice.setCustomerName("Alice");
        invoice.setAmount(new BigDecimal("99.00"));
        invoice.setCurrency("USDC");
        invoice.setStatus(InvoiceStatusEnum.EXPIRED);
        invoice.setExceptionTags(List.of("LATE_PAYMENT"));
        invoice.setExpireAt(OffsetDateTime.parse("2026-03-21T10:00:00Z"));
        invoice.setCreatedAt(OffsetDateTime.parse("2026-03-20T10:00:00Z"));

        Page<Invoice> page = new Page<>(2, 10);
        page.setTotal(1L);
        page.setRecords(List.of(invoice));
        return page;
    }
}
