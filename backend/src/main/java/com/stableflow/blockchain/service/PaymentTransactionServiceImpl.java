package com.stableflow.blockchain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.util.List;
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

    @Override
    public List<PaymentTransaction> listPendingVerificationTransactions(int limit) {
        int resolvedLimit = limit <= 0 ? 50 : limit;
        // 只挑出仍处于待验证状态的交易，并按时间正序返回，方便验证层稳定处理。
        return paymentTransactionMapper.selectList(
            new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getVerificationResult, PaymentVerificationResultEnum.PENDING)
                .orderByAsc(PaymentTransaction::getBlockTime)
                .orderByAsc(PaymentTransaction::getId)
                .last("LIMIT " + resolvedLimit)
        );
    }
}
