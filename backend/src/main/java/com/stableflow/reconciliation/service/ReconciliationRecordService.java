package com.stableflow.reconciliation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.reconciliation.entity.ReconciliationRecord;

public interface ReconciliationRecordService extends IService<ReconciliationRecord> {

    /** Check whether reconciliation has already been recorded for the invoice and transaction pair / 检查账单与交易组合是否已生成核销记录 */
    boolean existsByInvoiceIdAndTxHash(Long invoiceId, String txHash);

    /** Persist a reconciliation record only when the invoice and transaction pair has not been seen before / 仅在账单与交易组合未被处理过时保存核销记录 */
    boolean saveIfAbsent(ReconciliationRecord reconciliationRecord);

    /** Return the latest reconciliation record of the invoice / 返回账单最新核销记录 */
    ReconciliationRecord getLatestRecordByInvoiceId(Long invoiceId);
}
