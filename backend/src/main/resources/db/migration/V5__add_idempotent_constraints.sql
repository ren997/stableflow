ALTER TABLE reconciliation_record
    ADD CONSTRAINT uk_reconciliation_record_invoice_tx UNIQUE (invoice_id, tx_hash);

ALTER TABLE outbox_event
    ADD COLUMN last_error VARCHAR(512),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
