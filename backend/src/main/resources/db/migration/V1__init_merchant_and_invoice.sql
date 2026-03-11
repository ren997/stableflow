CREATE TABLE merchant (
    id BIGSERIAL PRIMARY KEY,
    merchant_name VARCHAR(128) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE merchant_payment_config (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL UNIQUE REFERENCES merchant(id),
    wallet_address VARCHAR(128) NOT NULL UNIQUE,
    mint_address VARCHAR(128) NOT NULL,
    chain VARCHAR(32) NOT NULL,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL REFERENCES merchant(id),
    invoice_no VARCHAR(64) NOT NULL UNIQUE,
    customer_name VARCHAR(128) NOT NULL,
    amount NUMERIC(36, 6) NOT NULL,
    currency VARCHAR(16) NOT NULL,
    chain VARCHAR(32) NOT NULL,
    description VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    exception_tags JSONB,
    expire_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoice_payment_request (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL UNIQUE REFERENCES invoice(id),
    recipient_address VARCHAR(128) NOT NULL,
    reference_key VARCHAR(128) NOT NULL UNIQUE,
    mint_address VARCHAR(128) NOT NULL,
    expected_amount NUMERIC(36, 6) NOT NULL,
    payment_link TEXT,
    label VARCHAR(128),
    message VARCHAR(255),
    expire_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
