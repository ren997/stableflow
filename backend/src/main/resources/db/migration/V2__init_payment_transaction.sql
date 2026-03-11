CREATE TABLE payment_transaction (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT REFERENCES invoice(id),
    tx_hash VARCHAR(128) NOT NULL UNIQUE,
    reference_key VARCHAR(128),
    payer_address VARCHAR(128),
    recipient_address VARCHAR(128) NOT NULL,
    amount NUMERIC(36, 6) NOT NULL,
    currency VARCHAR(16) NOT NULL,
    mint_address VARCHAR(128),
    block_time TIMESTAMPTZ,
    verification_result VARCHAR(32),
    payment_status VARCHAR(32),
    raw_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
