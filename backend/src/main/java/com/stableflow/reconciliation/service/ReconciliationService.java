package com.stableflow.reconciliation.service;

import com.stableflow.reconciliation.vo.ReconcileInvoiceVo;

public interface ReconciliationService {

    /** Reconcile a batch of verified transactions that have not produced reconciliation records yet / 批量核销尚未生成核销记录的已验证交易 */
    int reconcilePendingTransactions(int limit);

    /** Manually reconcile pending verified transactions for one owned invoice / 手动核销当前商家某张账单下待处理的已验证交易 */
    ReconcileInvoiceVo reconcileInvoice(Long invoiceId);
}
