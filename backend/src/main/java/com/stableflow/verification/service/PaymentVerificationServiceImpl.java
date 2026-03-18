package com.stableflow.verification.service;

import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.service.PaymentTransactionService;
import com.stableflow.verification.vo.PaymentVerificationResultVo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentVerificationServiceImpl implements PaymentVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentVerificationServiceImpl.class);

    private final PaymentTransactionService paymentTransactionService;
    private final SinglePaymentVerificationService singlePaymentVerificationService;

    @Override
    public int verifyPendingTransactions(int limit) {
        int processedCount = 0;
        // 先取出一批 PENDING 候选交易，再逐笔复用单交易验证规则。
        for (PaymentTransaction paymentTransaction : paymentTransactionService.listPendingVerificationTransactions(limit)) {
            try {
                singlePaymentVerificationService.verifyTransaction(paymentTransaction);
                processedCount++;
            } catch (RuntimeException ex) {
                // 单笔失败只记录日志，不阻断同批其它交易，避免任务吞掉后续可处理数据。
                log.error("Failed to verify paymentTransactionId={}, txHash={}", paymentTransaction.getId(), paymentTransaction.getTxHash(), ex);
            }
        }
        return processedCount;
    }

    @Override
    public PaymentVerificationResultVo verifyTransaction(Long paymentTransactionId) {
        return singlePaymentVerificationService.verifyTransaction(paymentTransactionId);
    }

    @Override
    public PaymentVerificationResultVo verifyTransaction(PaymentTransaction paymentTransaction) {
        return singlePaymentVerificationService.verifyTransaction(paymentTransaction);
    }
}
