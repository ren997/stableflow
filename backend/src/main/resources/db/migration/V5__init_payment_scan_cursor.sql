-- Address-level Solana payment scan cursor table / 地址级 Solana 支付扫描游标表
CREATE TABLE payment_scan_cursor (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Recipient wallet address / 收款钱包地址
    recipient_address VARCHAR(128) NOT NULL UNIQUE,
    -- Latest processed signature for incremental scanning / 最近一次已处理签名
    last_seen_signature VARCHAR(128),
    -- Last scan finished time in UTC / 最近一次扫描完成时间（UTC）
    last_scanned_at TIMESTAMPTZ,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Updated time in UTC / 更新时间（UTC）
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE payment_scan_cursor IS 'Address-level Solana payment scan cursor table / 地址级 Solana 支付扫描游标表';
COMMENT ON COLUMN payment_scan_cursor.id IS 'Primary key / 主键';
COMMENT ON COLUMN payment_scan_cursor.recipient_address IS 'Recipient wallet address / 收款钱包地址';
COMMENT ON COLUMN payment_scan_cursor.last_seen_signature IS 'Latest processed signature for incremental scanning / 最近一次已处理签名';
COMMENT ON COLUMN payment_scan_cursor.last_scanned_at IS 'Last scan finished time in UTC / 最近一次扫描完成时间（UTC）';
COMMENT ON COLUMN payment_scan_cursor.created_at IS 'Created time in UTC / 创建时间（UTC）';
COMMENT ON COLUMN payment_scan_cursor.updated_at IS 'Updated time in UTC / 更新时间（UTC）';
