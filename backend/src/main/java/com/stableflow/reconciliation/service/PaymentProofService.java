package com.stableflow.reconciliation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.blockchain.entity.PaymentTransaction;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.InvoiceStatusEnum;
import com.stableflow.reconciliation.entity.PaymentProof;
import com.stableflow.reconciliation.entity.ReconciliationRecord;
import com.stableflow.reconciliation.vo.PaymentProofVo;
import java.time.OffsetDateTime;

public interface PaymentProofService extends IService<PaymentProof> {

    /** Persist a payment proof snapshot only when the invoice and transaction pair has not been recorded yet / 仅在账单与交易组合尚未生成凭证时保存支付证明快照 */
    boolean saveIfAbsent(
        Invoice invoice,
        PaymentTransaction paymentTransaction,
        ReconciliationRecord reconciliationRecord,
        InvoiceStatusEnum finalStatus,
        String exceptionTags,
        OffsetDateTime paidAt
    );

    /** Return the latest payment proof for the current merchant invoice / 返回当前商家账单的最新支付凭证 */
    PaymentProofVo getLatestProof(Long invoiceId);
}
