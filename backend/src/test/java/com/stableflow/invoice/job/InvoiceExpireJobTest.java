package com.stableflow.invoice.job;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.system.config.InvoiceExpireJobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceExpireJobTest {

    @Mock
    private InvoiceService invoiceService;

    private InvoiceExpireJob invoiceExpireJob;

    @BeforeEach
    void setUp() {
        invoiceExpireJob = new InvoiceExpireJob(
            invoiceService,
            new InvoiceExpireJobProperties(true, 30_000L, 12_000L)
        );
    }

    @Test
    void shouldExpireInvoicesWhenJobIsEnabled() {
        when(invoiceService.expirePendingInvoices()).thenReturn(2);

        invoiceExpireJob.expirePendingInvoices();

        verify(invoiceService).expirePendingInvoices();
    }

    @Test
    void shouldSkipExpireSweepWhenJobIsDisabled() {
        invoiceExpireJob = new InvoiceExpireJob(
            invoiceService,
            new InvoiceExpireJobProperties(false, 30_000L, 12_000L)
        );

        invoiceExpireJob.expirePendingInvoices();

        verifyNoInteractions(invoiceService);
    }
}
