package com.stableflow.invoice.service;

import com.stableflow.invoice.dto.ManualSubmitPaymentRequestDto;
import com.stableflow.invoice.vo.ManualSubmitPaymentVo;

/** Manual payment submission service used as a fallback when wallet deep links are incompatible / 钱包深链兼容性不足时使用的手动支付提交服务 */
public interface ManualInvoicePaymentService {

    /** Submit one on-chain transaction hash for an owned invoice and try to verify plus reconcile it / 为当前商家账单提交一笔链上交易哈希并尝试验证与核销 */
    ManualSubmitPaymentVo submitPayment(ManualSubmitPaymentRequestDto request);
}
