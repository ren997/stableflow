package com.stableflow.blockchain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentTransactionServiceImpl
    extends ServiceImpl<PaymentTransactionMapper, PaymentTransaction>
    implements PaymentTransactionService {

    private final PaymentTransactionMapper paymentTransactionMapper;

    public PaymentTransactionServiceImpl(PaymentTransactionMapper paymentTransactionMapper) {
        this.paymentTransactionMapper = paymentTransactionMapper;
    }

    @Override
    public boolean existsByTxHash(String txHash) {
        return paymentTransactionMapper.selectCount(
            new LambdaQueryWrapper<PaymentTransaction>().eq(PaymentTransaction::getTxHash, txHash)
        ) > 0;
    }

    @Transactional
    @Override
    public boolean saveIfAbsent(PaymentTransaction paymentTransaction) {
        if (existsByTxHash(paymentTransaction.getTxHash())) {
            return false;
        }
        paymentTransactionMapper.insert(paymentTransaction);
        return true;
    }
}
