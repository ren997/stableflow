package com.stableflow.blockchain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.blockchain.entity.PaymentScanCursor;

public interface PaymentScanCursorService extends IService<PaymentScanCursor> {

    /** Return the existing cursor for a recipient address or create one / 获取指定收款地址的扫描游标，不存在时自动创建 */
    PaymentScanCursor getOrCreate(String recipientAddress);

    /** Persist the latest processed signature for incremental scanning / 持久化增量扫描使用的最新已处理签名 */
    void updateCursor(String recipientAddress, String lastSeenSignature);
}
