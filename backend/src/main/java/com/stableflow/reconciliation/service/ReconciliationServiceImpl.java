package com.stableflow.reconciliation.service;

import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
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
}
