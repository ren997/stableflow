package com.stableflow.reconciliation.service;

import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.invoice.service.InvoiceService;
import com.stableflow.reconciliation.vo.ReconcileInvoiceVo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

    private final PaymentTransactionService paymentTransactionService;
    private final SingleReconciliationService singleReconciliationService;
    private final InvoiceService invoiceService;

    @Override
    public int reconcilePendingTransactions(int limit) {
        int processedCount = 0;
        // 先挑出已验证但还没生成核销记录的交易，再逐笔执行原子核销。
        for (PaymentTransaction paymentTransaction : paymentTransactionService.listPendingReconciliationTransactions(limit)) {
            try {
                if (singleReconciliationService.reconcileTransaction(paymentTransaction)) {
                    processedCount++;
                }
            } catch (RuntimeException ex) {
                // 单笔核销失败只打日志，避免整批任务被一条脏数据阻断。
                log.error(
                    "Failed to reconcile paymentTransactionId={}, txHash={}",
                    paymentTransaction.getId(),
                    paymentTransaction.getTxHash(),
                    ex
                );
            }
        }
        return processedCount;
    }

    @Override
    public ReconcileInvoiceVo reconcileInvoice(Long invoiceId) {
        // 先校验账单归属，避免手动重放越权到账单外。
        PaymentStatusVo currentStatus = invoiceService.getPaymentStatus(invoiceId);
        List<PaymentTransaction> pendingTransactions = paymentTransactionService.listPendingReconciliationTransactionsByInvoiceId(invoiceId);
        if (pendingTransactions.isEmpty()) {
            return new ReconcileInvoiceVo(invoiceId, 0, currentStatus);
        }

        int processedCount = 0;
        // 对当前账单的待核销交易逐笔补跑，保持与定时任务一致的单笔幂等语义。
        for (PaymentTransaction paymentTransaction : pendingTransactions) {
            try {
                if (singleReconciliationService.reconcileTransaction(paymentTransaction)) {
                    processedCount++;
                }
            } catch (RuntimeException ex) {
                log.error(
                    "Failed to manually reconcile invoiceId={}, paymentTransactionId={}, txHash={}",
                    invoiceId,
                    paymentTransaction.getId(),
                    paymentTransaction.getTxHash(),
                    ex
                );
            }
        }
        return new ReconcileInvoiceVo(invoiceId, processedCount, invoiceService.getPaymentStatus(invoiceId));
    }
}
