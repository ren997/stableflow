package com.stableflow.blockchain.service;

import com.stableflow.merchant.entity.MerchantPaymentConfig;

public interface PaymentScanService {

    /** Scan all active merchant recipient addresses and persist new candidate transactions / 扫描所有启用中的商家收款地址并落库新的候选交易 */
    int scanAllActiveAddresses();

    /** Scan a single recipient address incrementally and persist newly discovered transactions / 按增量方式扫描单个收款地址并落库新发现的交易 */
    int scanRecipientAddress(MerchantPaymentConfig paymentConfig);
}
