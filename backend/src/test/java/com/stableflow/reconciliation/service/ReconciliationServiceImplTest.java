package com.stableflow.reconciliation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.reconciliation.vo.ReconcileInvoiceVo;
import com.stableflow.verification.enums.PaymentTransactionStatusEnum;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceImplTest {

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private SingleReconciliationService singleReconciliationService;

    @Mock
    private InvoiceService invoiceService;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationServiceImpl(
            paymentTransactionService,
            singleReconciliationService,
            invoiceService
        );
    }

    @Test
    void shouldManuallyReconcilePendingTransactionsForInvoice() {
        PaymentStatusVo beforeStatus = paymentStatusVo(100L, InvoiceStatusEnum.PENDING, "tx-before");
        PaymentStatusVo afterStatus = paymentStatusVo(100L, InvoiceStatusEnum.PAID, "tx-paid");
        PaymentTransaction first = transaction(100L, 11L, "tx-paid");
        PaymentTransaction second = transaction(100L, 12L, "tx-late");

        when(invoiceService.getPaymentStatus(100L)).thenReturn(beforeStatus, afterStatus);
        when(paymentTransactionService.listPendingReconciliationTransactionsByInvoiceId(100L)).thenReturn(List.of(first, second));
        when(singleReconciliationService.reconcileTransaction(first)).thenReturn(true);
        when(singleReconciliationService.reconcileTransaction(second)).thenReturn(false);

        ReconcileInvoiceVo response = reconciliationService.reconcileInvoice(100L);

        assertEquals(100L, response.invoiceId());
        assertEquals(1, response.reconciledCount());
        assertEquals(InvoiceStatusEnum.PAID, response.paymentStatus().status());
        verify(invoiceService, times(2)).getPaymentStatus(100L);
        verify(singleReconciliationService).reconcileTransaction(first);
        verify(singleReconciliationService).reconcileTransaction(second);
    }

    @Test
    void shouldReturnCurrentStatusWhenNoPendingTransactionsRemain() {
        PaymentStatusVo currentStatus = paymentStatusVo(101L, InvoiceStatusEnum.PENDING, "tx-existing");
        when(invoiceService.getPaymentStatus(101L)).thenReturn(currentStatus);
        when(paymentTransactionService.listPendingReconciliationTransactionsByInvoiceId(101L)).thenReturn(List.of());

        ReconcileInvoiceVo response = reconciliationService.reconcileInvoice(101L);

        assertEquals(0, response.reconciledCount());
        assertEquals("tx-existing", response.paymentStatus().latestTxHash());
    }

    private PaymentTransaction transaction(Long invoiceId, Long id, String txHash) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setId(id);
        paymentTransaction.setInvoiceId(invoiceId);
        paymentTransaction.setTxHash(txHash);
        return paymentTransaction;
    }

    private PaymentStatusVo paymentStatusVo(Long invoiceId, InvoiceStatusEnum invoiceStatus, String txHash) {
        return new PaymentStatusVo(
            invoiceId,
            "pub-" + invoiceId,
            "INV-" + invoiceId,
            invoiceStatus,
            List.of(),
            null,
            OffsetDateTime.parse("2026-03-20T10:00:00Z"),
            txHash,
            PaymentVerificationResultEnum.PAID,
            PaymentTransactionStatusEnum.PAID
        );
    }
}
