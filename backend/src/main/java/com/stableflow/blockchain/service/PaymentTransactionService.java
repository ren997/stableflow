package com.stableflow.blockchain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionMapper paymentTransactionMapper;

    public PaymentTransactionService(PaymentTransactionMapper paymentTransactionMapper) {
        this.paymentTransactionMapper = paymentTransactionMapper;
    }

    public boolean existsByTxHash(String txHash) {
        return paymentTransactionMapper.selectCount(
            new LambdaQueryWrapper<PaymentTransaction>().eq(PaymentTransaction::getTxHash, txHash)
        ) > 0;
    }

    @Transactional
    public boolean saveIfAbsent(PaymentTransaction paymentTransaction) {
        if (existsByTxHash(paymentTransaction.getTxHash())) {
            return false;
        }
        paymentTransactionMapper.insert(paymentTransaction);
        return true;
    }
}
