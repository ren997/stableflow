package com.stableflow.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import com.stableflow.dashboard.vo.DashboardInvoiceStatusDistributionVo;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.system.security.CurrentMerchantProvider;
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
}
