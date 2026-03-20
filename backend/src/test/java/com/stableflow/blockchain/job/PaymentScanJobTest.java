package com.stableflow.blockchain.job;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.service.PaymentScanService;
import com.stableflow.system.config.SolanaScanProperties;
import com.stableflow.system.lock.JobLockService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentScanJobTest {

    @Mock
    private PaymentScanService paymentScanService;

    @Mock
    private JobLockService jobLockService;

    private PaymentScanJob paymentScanJob;

    @BeforeEach
    void setUp() {
        paymentScanJob = new PaymentScanJob(
            paymentScanService,
            new SolanaScanProperties(
                true,
                50,
                30_000L,
                10_000L,
                true,
                "stableflow:job:payment-scan:lock",
                Duration.ofMinutes(5)
            ),
            jobLockService
        );
    }

    @Test
    void shouldRunScanWhenLockAcquired() {
        when(jobLockService.tryLock(eq("stableflow:job:payment-scan:lock"), any(), eq(Duration.ofMinutes(5))))
            .thenReturn(true);
        when(paymentScanService.scanAllActiveAddresses()).thenReturn(2);

        paymentScanJob.scanPayments();

        verify(paymentScanService).scanAllActiveAddresses();
        verify(jobLockService).unlock(eq("stableflow:job:payment-scan:lock"), any());
    }

    @Test
    void shouldSkipScanWhenLockAcquisitionFails() {
        when(jobLockService.tryLock(eq("stableflow:job:payment-scan:lock"), any(), eq(Duration.ofMinutes(5))))
            .thenReturn(false);

        paymentScanJob.scanPayments();

        verifyNoInteractions(paymentScanService);
    }

    @Test
    void shouldReleaseLockWhenScanThrowsException() {
        when(jobLockService.tryLock(eq("stableflow:job:payment-scan:lock"), any(), eq(Duration.ofMinutes(5))))
            .thenReturn(true);
        when(paymentScanService.scanAllActiveAddresses()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> paymentScanJob.scanPayments());

        verify(jobLockService).unlock(eq("stableflow:job:payment-scan:lock"), any());
    }

    @Test
    void shouldSkipEntireJobWhenSchedulingDisabled() {
        paymentScanJob = new PaymentScanJob(
            paymentScanService,
            new SolanaScanProperties(false, 50, 30_000L, 10_000L, true, "stableflow:job:payment-scan:lock", Duration.ofMinutes(5)),
            jobLockService
        );

        paymentScanJob.scanPayments();

        verifyNoInteractions(paymentScanService, jobLockService);
    }
}
