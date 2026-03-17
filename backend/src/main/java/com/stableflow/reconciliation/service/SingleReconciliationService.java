package com.stableflow.reconciliation.service;

import com.stableflow.blockchain.entity.PaymentTransaction;

public interface SingleReconciliationService {

    /** Reconcile one verified transaction and update invoice state atomically / 原子地核销单笔已验证交易并更新账单状态 */
    boolean reconcileTransaction(PaymentTransaction paymentTransaction);
}
