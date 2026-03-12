-- Index for verification queue scans / 验证队列扫描索引
CREATE INDEX idx_payment_transaction_verification_result_created_at
    ON payment_transaction(verification_result, created_at);

-- Index for payment status queries / 支付状态查询索引
CREATE INDEX idx_payment_transaction_payment_status
    ON payment_transaction(payment_status);

-- Index for reconciliation lookup by transaction hash / 核销交易哈希查询索引
CREATE INDEX idx_reconciliation_record_tx_hash
    ON reconciliation_record(tx_hash);

COMMENT ON INDEX idx_payment_transaction_verification_result_created_at
    IS 'Index for verification queue scans / 验证队列扫描索引';

COMMENT ON INDEX idx_payment_transaction_payment_status
    IS 'Index for payment status queries / 支付状态查询索引';

COMMENT ON INDEX idx_reconciliation_record_tx_hash
    IS 'Index for reconciliation lookup by transaction hash / 核销交易哈希查询索引';
