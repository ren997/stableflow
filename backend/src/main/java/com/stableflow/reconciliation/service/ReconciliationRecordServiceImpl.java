package com.stableflow.reconciliation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.mapper.ReconciliationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReconciliationRecordServiceImpl
    extends ServiceImpl<ReconciliationRecordMapper, ReconciliationRecord>
    implements ReconciliationRecordService {

    private final ReconciliationRecordMapper reconciliationRecordMapper;

    @Override
    public boolean existsByInvoiceIdAndTxHash(Long invoiceId, String txHash) {
        return reconciliationRecordMapper.selectCount(
            new LambdaQueryWrapper<ReconciliationRecord>()
                .eq(ReconciliationRecord::getInvoiceId, invoiceId)
                .eq(ReconciliationRecord::getTxHash, txHash)
        ) > 0;
    }

    @Transactional
    @Override
    public boolean saveIfAbsent(ReconciliationRecord reconciliationRecord) {
        if (existsByInvoiceIdAndTxHash(reconciliationRecord.getInvoiceId(), reconciliationRecord.getTxHash())) {
            return false;
        }
        reconciliationRecordMapper.insert(reconciliationRecord);
        return true;
    }

    @Override
    public ReconciliationRecord getLatestRecordByInvoiceId(Long invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        return reconciliationRecordMapper.selectOne(
            new LambdaQueryWrapper<ReconciliationRecord>()
                .eq(ReconciliationRecord::getInvoiceId, invoiceId)
                .orderByDesc(ReconciliationRecord::getProcessedAt)
                .orderByDesc(ReconciliationRecord::getCreatedAt)
                .orderByDesc(ReconciliationRecord::getId)
                .last("LIMIT 1")
        );
    }
}
