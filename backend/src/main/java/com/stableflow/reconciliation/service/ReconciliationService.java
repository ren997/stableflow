package com.stableflow.reconciliation.service;

public interface ReconciliationService {

    /** Reconcile a batch of verified transactions that have not produced reconciliation records yet / 批量核销尚未生成核销记录的已验证交易 */
    int reconcilePendingTransactions(int limit);
}
