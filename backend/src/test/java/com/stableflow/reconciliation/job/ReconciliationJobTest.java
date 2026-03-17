package com.stableflow.reconciliation.job;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stableflow.reconciliation.service.ReconciliationService;
import com.stableflow.system.config.ReconciliationJobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationJobTest {

    @Mock
    private ReconciliationService reconciliationService;

    private ReconciliationJob reconciliationJob;

    @BeforeEach
    void setUp() {
        reconciliationJob = new ReconciliationJob(
            reconciliationService,
            new ReconciliationJobProperties(true, 20, 30_000L, 20_000L)
        );
    }

    @Test
    void shouldRunReconciliationWhenJobIsEnabled() {
        when(reconciliationService.reconcilePendingTransactions(20)).thenReturn(2);

        reconciliationJob.reconcileVerifiedPayments();

        verify(reconciliationService).reconcilePendingTransactions(20);
    }

    @Test
    void shouldSkipReconciliationWhenJobIsDisabled() {
        reconciliationJob = new ReconciliationJob(
            reconciliationService,
            new ReconciliationJobProperties(false, 20, 30_000L, 20_000L)
        );

        reconciliationJob.reconcileVerifiedPayments();

        verifyNoInteractions(reconciliationService);
    }
}
