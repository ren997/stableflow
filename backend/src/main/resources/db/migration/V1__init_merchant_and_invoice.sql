-- Merchant master table / 商家主表
CREATE TABLE merchant (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Merchant display name / 商家名称
    merchant_name VARCHAR(128) NOT NULL,
    -- Merchant login email / 商家登录邮箱
    email VARCHAR(255) NOT NULL UNIQUE,
    -- Hashed password / 密码哈希
    password_hash VARCHAR(255) NOT NULL,
    -- Merchant status / 商家状态
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Updated time in UTC / 更新时间（UTC）
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Merchant fixed payment address configuration / 商家固定收款地址配置表
CREATE TABLE merchant_payment_config (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Referenced merchant id / 关联商家 ID
    merchant_id BIGINT NOT NULL UNIQUE REFERENCES merchant(id),
    -- Fixed recipient wallet address / 固定收款钱包地址
    wallet_address VARCHAR(128) NOT NULL UNIQUE,
    -- Token mint address / 代币 Mint 地址
    mint_address VARCHAR(128) NOT NULL,
    -- Blockchain name / 链名称
    chain VARCHAR(32) NOT NULL,
    -- Whether current config is active / 当前配置是否启用
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Updated time in UTC / 更新时间（UTC）
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Invoice master table / 账单主表
CREATE TABLE invoice (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Public invoice identifier / 对外公开账单标识
    public_id VARCHAR(64) NOT NULL UNIQUE,
    -- Referenced merchant id / 关联商家 ID
    merchant_id BIGINT NOT NULL REFERENCES merchant(id),
    -- Business invoice number / 业务账单编号
    invoice_no VARCHAR(64) NOT NULL UNIQUE,
    -- Customer display name / 客户名称
    customer_name VARCHAR(128) NOT NULL,
    -- Expected invoice amount / 账单应付金额
    amount NUMERIC(36, 6) NOT NULL,
    -- Invoice currency code / 账单币种代码
    currency VARCHAR(16) NOT NULL,
    -- Blockchain name / 链名称
    chain VARCHAR(32) NOT NULL,
    -- Invoice description / 账单描述
    description VARCHAR(512),
    -- Invoice status / 账单状态
    status VARCHAR(32) NOT NULL,
    -- Exception tag list in JSONB / 异常标签 JSONB 列表
    exception_tags JSONB,
    -- Invoice expiry time in UTC / 账单过期时间（UTC）
    expire_at TIMESTAMPTZ NOT NULL,
    -- Paid time in UTC / 支付时间（UTC）
    paid_at TIMESTAMPTZ,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Updated time in UTC / 更新时间（UTC）
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Invoice payment request snapshot / 账单支付请求快照表
CREATE TABLE invoice_payment_request (
    -- Primary key / 主键
    id BIGSERIAL PRIMARY KEY,
    -- Referenced invoice id / 关联账单 ID
    invoice_id BIGINT NOT NULL UNIQUE REFERENCES invoice(id),
    -- Recipient wallet address / 收款地址
    recipient_address VARCHAR(128) NOT NULL,
    -- Unique payment reference key / 唯一支付 reference
    reference_key VARCHAR(128) NOT NULL UNIQUE,
    -- Token mint address / 代币 Mint 地址
    mint_address VARCHAR(128) NOT NULL,
    -- Expected payment amount / 应付金额
    expected_amount NUMERIC(36, 6) NOT NULL,
    -- Generated payment link / 生成的支付链接
    payment_link TEXT,
    -- Display label for wallet / 钱包展示标签
    label VARCHAR(128),
    -- Display message for wallet / 钱包展示消息
    message VARCHAR(255),
    -- Payment request expiry time in UTC / 支付请求过期时间（UTC）
    expire_at TIMESTAMPTZ NOT NULL,
    -- Created time in UTC / 创建时间（UTC）
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for merchant invoice status queries / 商家账单状态查询索引
CREATE INDEX idx_invoice_merchant_status ON invoice(merchant_id, status);
-- Index for invoice expiry scans / 账单过期扫描索引
CREATE INDEX idx_invoice_expire_at ON invoice(expire_at);

COMMENT ON TABLE merchant IS 'Merchant master table / 商家主表';
COMMENT ON COLUMN merchant.id IS 'Primary key / 主键';
COMMENT ON COLUMN merchant.merchant_name IS 'Merchant display name / 商家名称';
COMMENT ON COLUMN merchant.email IS 'Merchant login email / 商家登录邮箱';
COMMENT ON COLUMN merchant.password_hash IS 'Hashed password / 密码哈希';
COMMENT ON COLUMN merchant.status IS 'Merchant status / 商家状态';
COMMENT ON COLUMN merchant.created_at IS 'Created time in UTC / 创建时间（UTC）';
COMMENT ON COLUMN merchant.updated_at IS 'Updated time in UTC / 更新时间（UTC）';

COMMENT ON TABLE merchant_payment_config IS 'Merchant fixed payment address configuration / 商家固定收款地址配置表';
COMMENT ON COLUMN merchant_payment_config.id IS 'Primary key / 主键';
COMMENT ON COLUMN merchant_payment_config.merchant_id IS 'Referenced merchant id / 关联商家 ID';
COMMENT ON COLUMN merchant_payment_config.wallet_address IS 'Fixed recipient wallet address / 固定收款钱包地址';
COMMENT ON COLUMN merchant_payment_config.mint_address IS 'Token mint address / 代币 Mint 地址';
COMMENT ON COLUMN merchant_payment_config.chain IS 'Blockchain name / 链名称';
COMMENT ON COLUMN merchant_payment_config.active_flag IS 'Whether current config is active / 当前配置是否启用';
COMMENT ON COLUMN merchant_payment_config.created_at IS 'Created time in UTC / 创建时间（UTC）';
COMMENT ON COLUMN merchant_payment_config.updated_at IS 'Updated time in UTC / 更新时间（UTC）';

COMMENT ON TABLE invoice IS 'Invoice master table / 账单主表';
COMMENT ON COLUMN invoice.id IS 'Primary key / 主键';
COMMENT ON COLUMN invoice.public_id IS 'Public invoice identifier / 对外公开账单标识';
COMMENT ON COLUMN invoice.merchant_id IS 'Referenced merchant id / 关联商家 ID';
COMMENT ON COLUMN invoice.invoice_no IS 'Business invoice number / 业务账单编号';
COMMENT ON COLUMN invoice.customer_name IS 'Customer display name / 客户名称';
COMMENT ON COLUMN invoice.amount IS 'Expected invoice amount / 账单应付金额';
COMMENT ON COLUMN invoice.currency IS 'Invoice currency code / 账单币种代码';
COMMENT ON COLUMN invoice.chain IS 'Blockchain name / 链名称';
COMMENT ON COLUMN invoice.description IS 'Invoice description / 账单描述';
COMMENT ON COLUMN invoice.status IS 'Invoice status / 账单状态';
COMMENT ON COLUMN invoice.exception_tags IS 'Exception tag list in JSONB / 异常标签 JSONB 列表';
COMMENT ON COLUMN invoice.expire_at IS 'Invoice expiry time in UTC / 账单过期时间（UTC）';
COMMENT ON COLUMN invoice.paid_at IS 'Paid time in UTC / 支付时间（UTC）';
COMMENT ON COLUMN invoice.created_at IS 'Created time in UTC / 创建时间（UTC）';
COMMENT ON COLUMN invoice.updated_at IS 'Updated time in UTC / 更新时间（UTC）';

COMMENT ON TABLE invoice_payment_request IS 'Invoice payment request snapshot / 账单支付请求快照表';
COMMENT ON COLUMN invoice_payment_request.id IS 'Primary key / 主键';
COMMENT ON COLUMN invoice_payment_request.invoice_id IS 'Referenced invoice id / 关联账单 ID';
COMMENT ON COLUMN invoice_payment_request.recipient_address IS 'Recipient wallet address / 收款地址';
COMMENT ON COLUMN invoice_payment_request.reference_key IS 'Unique payment reference key / 唯一支付 reference';
COMMENT ON COLUMN invoice_payment_request.mint_address IS 'Token mint address / 代币 Mint 地址';
COMMENT ON COLUMN invoice_payment_request.expected_amount IS 'Expected payment amount / 应付金额';
COMMENT ON COLUMN invoice_payment_request.payment_link IS 'Generated payment link / 生成的支付链接';
COMMENT ON COLUMN invoice_payment_request.label IS 'Display label for wallet / 钱包展示标签';
COMMENT ON COLUMN invoice_payment_request.message IS 'Display message for wallet / 钱包展示消息';
COMMENT ON COLUMN invoice_payment_request.expire_at IS 'Payment request expiry time in UTC / 支付请求过期时间（UTC）';
COMMENT ON COLUMN invoice_payment_request.created_at IS 'Created time in UTC / 创建时间（UTC）';

COMMENT ON INDEX idx_invoice_merchant_status IS 'Index for merchant invoice status queries / 商家账单状态查询索引';
COMMENT ON INDEX idx_invoice_expire_at IS 'Index for invoice expiry scans / 账单过期扫描索引';
