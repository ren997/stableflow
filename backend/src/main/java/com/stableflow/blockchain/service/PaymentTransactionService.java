package com.stableflow.blockchain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.blockchain.entity.PaymentTransaction;
import java.util.List;

public interface PaymentTransactionService extends IService<PaymentTransaction> {

    /** Check whether a blockchain transaction has already been persisted / 检查链上交易是否已经落库 */
    boolean existsByTxHash(String txHash);

    /** Load one persisted transaction by blockchain hash / 按链上交易哈希加载已落库交易 */
    PaymentTransaction getByTxHash(String txHash);

    /** Persist a transaction only when its hash has not been seen before / 仅在交易哈希未出现过时落库 */
    boolean saveIfAbsent(PaymentTransaction paymentTransaction);

    /** List candidate transactions that are still waiting for verification / 查询仍处于待验证状态的候选交易 */
    List<PaymentTransaction> listPendingVerificationTransactions(int limit);

    /** List verified transactions that still have not been consumed by reconciliation / 查询已验证但尚未被核销消费的交易 */
    List<PaymentTransaction> listPendingReconciliationTransactions(int limit);

    /** List verified unreconciled transactions for one invoice / 查询单张账单下已验证但尚未被核销消费的交易 */
    List<PaymentTransaction> listPendingReconciliationTransactionsByInvoiceId(Long invoiceId);

    /** Return the latest transaction associated with the invoice / 返回账单关联的最新交易 */
    PaymentTransaction getLatestTransactionByInvoiceId(Long invoiceId);
}
