package com.stableflow.reconciliation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.mapper.ReconciliationRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationRecordServiceImpl
    extends ServiceImpl<ReconciliationRecordMapper, ReconciliationRecord>
    implements ReconciliationRecordService {

    private final ReconciliationRecordMapper reconciliationRecordMapper;

    public ReconciliationRecordServiceImpl(ReconciliationRecordMapper reconciliationRecordMapper) {
        this.reconciliationRecordMapper = reconciliationRecordMapper;
    }

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
}
