-- Reconciliation processing result table / 核销处理结果表
CREATE TABLE reconciliation_record (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Referenced invoice id / 关联账单 ID
    invoice_id BIGINT NOT NULL REFERENCES invoice(id),
    -- Blockchain transaction hash / 链上交易哈希
    tx_hash VARCHAR(128) NOT NULL,
    -- Reconciliation status / 核销状态
    reconciliation_status VARCHAR(32) NOT NULL,
    -- Reconciliation result message / 核销结果说明
    result_message VARCHAR(512),
    -- Exception tag list in JSONB / 异常标签 JSONB 列表
    exception_tags JSONB,
    -- Processed time in UTC / 处理时间（UTC）
    processed_at TIMESTAMPTZ,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reconciliation_invoice_tx UNIQUE (invoice_id, tx_hash)
);

-- Payment proof snapshot table / 支付凭证快照表
CREATE TABLE payment_proof (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Referenced invoice id / 关联账单 ID
    invoice_id BIGINT NOT NULL REFERENCES invoice(id),
    -- Blockchain transaction hash / 链上交易哈希
    tx_hash VARCHAR(128) NOT NULL,
    -- Proof type / 凭证类型
    proof_type VARCHAR(32) NOT NULL,
    -- Proof payload in JSONB / 凭证载荷 JSONB
    proof_payload JSONB NOT NULL,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Reliable outbox event table / 可靠事件 Outbox 表
CREATE TABLE outbox_event (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Event type / 事件类型
    event_type VARCHAR(64) NOT NULL,
    -- Aggregate type / 聚合根类型
    aggregate_type VARCHAR(64) NOT NULL,
    -- Aggregate identifier / 聚合根标识
    aggregate_id VARCHAR(64) NOT NULL,
    -- Event payload in JSONB / 事件载荷 JSONB
    payload JSONB NOT NULL,
    -- Dispatch status / 分发状态
    status VARCHAR(32) NOT NULL,
    -- Retry count / 重试次数
    retry_count INT NOT NULL DEFAULT 0,
    -- Last dispatch error message / 最近一次分发错误信息
    last_error TEXT,
    -- Next retry time in UTC / 下次重试时间（UTC）
    next_retry_at TIMESTAMPTZ,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Updated time in UTC / 更新时间（UTC）
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for payment proof invoice queries / 支付凭证账单查询索引
CREATE INDEX idx_payment_proof_invoice_id ON payment_proof(invoice_id);
-- Index for payment proof transaction lookups / 支付凭证交易查询索引
CREATE INDEX idx_payment_proof_tx_hash ON payment_proof(tx_hash);
-- Index for outbox dispatch and retry scans / Outbox 分发与重试扫描索引
CREATE INDEX idx_outbox_event_status_next_retry ON outbox_event(status, next_retry_at);

COMMENT ON TABLE reconciliation_record IS 'Reconciliation processing result table / 核销处理结果表';
COMMENT ON COLUMN reconciliation_record.id IS 'Primary key / 主键';
COMMENT ON COLUMN reconciliation_record.invoice_id IS 'Referenced invoice id / 关联账单 ID';
COMMENT ON COLUMN reconciliation_record.tx_hash IS 'Blockchain transaction hash / 链上交易哈希';
COMMENT ON COLUMN reconciliation_record.reconciliation_status IS 'Reconciliation status / 核销状态';
COMMENT ON COLUMN reconciliation_record.result_message IS 'Reconciliation result message / 核销结果说明';
COMMENT ON COLUMN reconciliation_record.exception_tags IS 'Exception tag list in JSONB / 异常标签 JSONB 列表';
COMMENT ON COLUMN reconciliation_record.processed_at IS 'Processed time in UTC / 处理时间（UTC）';
COMMENT ON COLUMN reconciliation_record.created_at IS 'Created time in UTC / 创建时间（UTC）';

COMMENT ON TABLE payment_proof IS 'Payment proof snapshot table / 支付凭证快照表';
COMMENT ON COLUMN payment_proof.id IS 'Primary key / 主键';
COMMENT ON COLUMN payment_proof.invoice_id IS 'Referenced invoice id / 关联账单 ID';
COMMENT ON COLUMN payment_proof.tx_hash IS 'Blockchain transaction hash / 链上交易哈希';
COMMENT ON COLUMN payment_proof.proof_type IS 'Proof type / 凭证类型';
COMMENT ON COLUMN payment_proof.proof_payload IS 'Proof payload in JSONB / 凭证载荷 JSONB';
COMMENT ON COLUMN payment_proof.created_at IS 'Created time in UTC / 创建时间（UTC）';

COMMENT ON TABLE outbox_event IS 'Reliable outbox event table / 可靠事件 Outbox 表';
COMMENT ON COLUMN outbox_event.id IS 'Primary key / 主键';
COMMENT ON COLUMN outbox_event.event_type IS 'Event type / 事件类型';
COMMENT ON COLUMN outbox_event.aggregate_type IS 'Aggregate type / 聚合根类型';
COMMENT ON COLUMN outbox_event.aggregate_id IS 'Aggregate identifier / 聚合根标识';
COMMENT ON COLUMN outbox_event.payload IS 'Event payload in JSONB / 事件载荷 JSONB';
COMMENT ON COLUMN outbox_event.status IS 'Dispatch status / 分发状态';
COMMENT ON COLUMN outbox_event.retry_count IS 'Retry count / 重试次数';
COMMENT ON COLUMN outbox_event.last_error IS 'Last dispatch error message / 最近一次分发错误信息';
COMMENT ON COLUMN outbox_event.next_retry_at IS 'Next retry time in UTC / 下次重试时间（UTC）';
COMMENT ON COLUMN outbox_event.created_at IS 'Created time in UTC / 创建时间（UTC）';
COMMENT ON COLUMN outbox_event.updated_at IS 'Updated time in UTC / 更新时间（UTC）';

COMMENT ON CONSTRAINT uk_reconciliation_invoice_tx ON reconciliation_record IS 'Unique reconciliation per invoice and transaction / 每张账单与交易唯一核销约束';
COMMENT ON INDEX idx_payment_proof_invoice_id IS 'Index for payment proof invoice queries / 支付凭证账单查询索引';
COMMENT ON INDEX idx_payment_proof_tx_hash IS 'Index for payment proof transaction lookups / 支付凭证交易查询索引';
COMMENT ON INDEX idx_outbox_event_status_next_retry IS 'Index for outbox dispatch and retry scans / Outbox 分发与重试扫描索引';
