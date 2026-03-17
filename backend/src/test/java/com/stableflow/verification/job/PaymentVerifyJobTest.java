package com.stableflow.verification.job;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stableflow.system.config.PaymentVerifyProperties;
import com.stableflow.verification.service.PaymentVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentVerifyJobTest {

    @Mock
    private PaymentVerificationService paymentVerificationService;

    private PaymentVerifyJob paymentVerifyJob;

    @BeforeEach
    void setUp() {
        paymentVerifyJob = new PaymentVerifyJob(
            paymentVerificationService,
            new PaymentVerifyProperties(true, 20, 30_000L, 15_000L)
        );
    }

    @Test
    void shouldRunVerificationWhenJobIsEnabled() {
        when(paymentVerificationService.verifyPendingTransactions(20)).thenReturn(2);

        paymentVerifyJob.verifyPendingPayments();

        verify(paymentVerificationService).verifyPendingTransactions(20);
    }

    @Test
    void shouldSkipVerificationWhenJobIsDisabled() {
        paymentVerifyJob = new PaymentVerifyJob(
            paymentVerificationService,
            new PaymentVerifyProperties(false, 20, 30_000L, 15_000L)
        );

        paymentVerifyJob.verifyPendingPayments();

        verifyNoInteractions(paymentVerificationService);
    }

    @Test
    void shouldFallbackToDefaultBatchSizeWhenConfigIsInvalid() {
        paymentVerifyJob = new PaymentVerifyJob(
            paymentVerificationService,
            new PaymentVerifyProperties(true, 0, 30_000L, 15_000L)
        );
        when(paymentVerificationService.verifyPendingTransactions(50)).thenReturn(1);

        paymentVerifyJob.verifyPendingPayments();

        verify(paymentVerificationService).verifyPendingTransactions(50);
    }
}
