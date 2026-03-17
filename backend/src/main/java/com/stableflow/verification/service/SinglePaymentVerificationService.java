package com.stableflow.verification.service;

import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.verification.vo.PaymentVerificationResultVo;

public interface SinglePaymentVerificationService {

    /** Verify one persisted candidate transaction by id within an isolated transaction / 在独立事务内按主键验证单笔候选交易 */
    PaymentVerificationResultVo verifyTransaction(Long paymentTransactionId);

    /** Verify one loaded candidate transaction within an isolated transaction / 在独立事务内验证单笔已加载候选交易 */
    PaymentVerificationResultVo verifyTransaction(PaymentTransaction paymentTransaction);
}
