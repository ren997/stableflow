-- Detected on-chain payment transaction table / 链上识别支付交易表
CREATE TABLE payment_transaction (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Associated invoice id if matched / 关联账单 ID（若已匹配）
    invoice_id BIGINT REFERENCES invoice(id),
    -- Blockchain transaction hash / 链上交易哈希
    tx_hash VARCHAR(128) NOT NULL UNIQUE,
    -- Parsed payment reference key / 解析出的支付 reference
    reference_key VARCHAR(128),
    -- Payer wallet address / 付款钱包地址
    payer_address VARCHAR(128),
    -- Recipient wallet address / 收款钱包地址
    recipient_address VARCHAR(128) NOT NULL,
    -- Transferred amount / 转账金额
    amount NUMERIC(36, 6) NOT NULL,
    -- Transferred currency code / 转账币种代码
    currency VARCHAR(16) NOT NULL,
    -- Transferred token mint address / 转账代币 Mint 地址
    mint_address VARCHAR(128),
    -- Blockchain block time in UTC / 链上区块时间（UTC）
    block_time TIMESTAMPTZ,
    -- Verification result / 验证结果
    verification_result VARCHAR(32),
    -- Derived payment status / 派生支付状态
    payment_status VARCHAR(32),
    -- Raw blockchain payload in JSONB / 原始链上载荷 JSONB
    raw_payload JSONB,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for invoice transaction lookups / 账单交易查询索引
CREATE INDEX idx_payment_transaction_invoice_id ON payment_transaction(invoice_id);
-- Index for reference-based matching / reference 匹配索引
CREATE INDEX idx_payment_transaction_reference_key ON payment_transaction(reference_key);
-- Index for recipient and block time scans / 收款地址与区块时间扫描索引
CREATE INDEX idx_payment_transaction_recipient_block_time ON payment_transaction(recipient_address, block_time);

COMMENT ON TABLE payment_transaction IS 'Detected on-chain payment transaction table / 链上识别支付交易表';
COMMENT ON COLUMN payment_transaction.id IS 'Primary key / 主键';
COMMENT ON COLUMN payment_transaction.invoice_id IS 'Associated invoice id if matched / 关联账单 ID（若已匹配）';
COMMENT ON COLUMN payment_transaction.tx_hash IS 'Blockchain transaction hash / 链上交易哈希';
COMMENT ON COLUMN payment_transaction.reference_key IS 'Parsed payment reference key / 解析出的支付 reference';
COMMENT ON COLUMN payment_transaction.payer_address IS 'Payer wallet address / 付款钱包地址';
COMMENT ON COLUMN payment_transaction.recipient_address IS 'Recipient wallet address / 收款钱包地址';
COMMENT ON COLUMN payment_transaction.amount IS 'Transferred amount / 转账金额';
COMMENT ON COLUMN payment_transaction.currency IS 'Transferred currency code / 转账币种代码';
COMMENT ON COLUMN payment_transaction.mint_address IS 'Transferred token mint address / 转账代币 Mint 地址';
COMMENT ON COLUMN payment_transaction.block_time IS 'Blockchain block time in UTC / 链上区块时间（UTC）';
COMMENT ON COLUMN payment_transaction.verification_result IS 'Verification result / 验证结果';
COMMENT ON COLUMN payment_transaction.payment_status IS 'Derived payment status / 派生支付状态';
COMMENT ON COLUMN payment_transaction.raw_payload IS 'Raw blockchain payload in JSONB / 原始链上载荷 JSONB';
COMMENT ON COLUMN payment_transaction.created_at IS 'Created time in UTC / 创建时间（UTC）';

COMMENT ON INDEX idx_payment_transaction_invoice_id IS 'Index for invoice transaction lookups / 账单交易查询索引';
COMMENT ON INDEX idx_payment_transaction_reference_key IS 'Index for reference-based matching / reference 匹配索引';
COMMENT ON INDEX idx_payment_transaction_recipient_block_time IS 'Index for recipient and block time scans / 收款地址与区块时间扫描索引';
