package com.stableflow.blockchain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.blockchain.mapper.PaymentTransactionMapper;
import com.stableflow.reconciliation.service.ReconciliationRecordService;
import com.stableflow.verification.enums.PaymentVerificationResultEnum;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl
    extends ServiceImpl<PaymentTransactionMapper, PaymentTransaction>
    implements PaymentTransactionService {

    private final PaymentTransactionMapper paymentTransactionMapper;
    private final ReconciliationRecordService reconciliationRecordService;

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

    @Override
    public List<PaymentTransaction> listPendingReconciliationTransactions(int limit) {
        int resolvedLimit = limit <= 0 ? 50 : limit;
        long lastSeenId = 0L;
        List<PaymentTransaction> pendingTransactions = new ArrayList<>();

        while (pendingTransactions.size() < resolvedLimit) {
            // 先按通用业务条件筛出已验证交易，再在业务层补“是否已核销”的规则判断。
            List<PaymentTransaction> candidates = paymentTransactionMapper.selectList(
                new LambdaQueryWrapper<PaymentTransaction>()
                    .isNotNull(PaymentTransaction::getInvoiceId)
                    .ne(PaymentTransaction::getVerificationResult, PaymentVerificationResultEnum.PENDING)
                    .gt(PaymentTransaction::getId, lastSeenId)
                    .orderByAsc(PaymentTransaction::getId)
                    .last("LIMIT " + resolvedLimit)
            );
            if (candidates.isEmpty()) {
                break;
            }

            for (PaymentTransaction candidate : candidates) {
                lastSeenId = candidate.getId();
                if (candidate.getInvoiceId() == null) {
                    continue;
                }
                if (reconciliationRecordService.existsByInvoiceIdAndTxHash(candidate.getInvoiceId(), candidate.getTxHash())) {
                    continue;
                }
                pendingTransactions.add(candidate);
                if (pendingTransactions.size() >= resolvedLimit) {
                    break;
                }
            }
        }

        return pendingTransactions;
    }

    @Override
    public PaymentTransaction getLatestTransactionByInvoiceId(Long invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        return paymentTransactionMapper.selectOne(
            new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getInvoiceId, invoiceId)
                .orderByDesc(PaymentTransaction::getBlockTime)
                .orderByDesc(PaymentTransaction::getCreatedAt)
                .orderByDesc(PaymentTransaction::getId)
                .last("LIMIT 1")
        );
    }
}
