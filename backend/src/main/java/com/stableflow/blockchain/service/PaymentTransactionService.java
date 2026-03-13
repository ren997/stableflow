package com.stableflow.blockchain.service;

import com.stableflow.blockchain.entity.PaymentTransaction;

public interface PaymentTransactionService {

    /** Check whether a blockchain transaction has already been persisted / 检查链上交易是否已经落库 */
    boolean existsByTxHash(String txHash);

    /** Persist a transaction only when its hash has not been seen before / 仅在交易哈希未出现过时落库 */
    boolean saveIfAbsent(PaymentTransaction paymentTransaction);
}
