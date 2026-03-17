package com.stableflow.verification.service;

import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.verification.vo.PaymentVerificationResultVo;

public interface PaymentVerificationService {

    /** Verify a persisted candidate transaction by id and write back the verification result / 按交易主键验证候选交易并回写验证结果 */
    PaymentVerificationResultVo verifyTransaction(Long paymentTransactionId);

    /** Verify a loaded candidate transaction and write back the verification result when the primary key exists / 验证已加载的候选交易，若存在主键则回写验证结果 */
    PaymentVerificationResultVo verifyTransaction(PaymentTransaction paymentTransaction);
}
