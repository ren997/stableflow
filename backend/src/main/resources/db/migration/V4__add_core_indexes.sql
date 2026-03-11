CREATE INDEX idx_invoice_merchant_status ON invoice(merchant_id, status);
CREATE INDEX idx_invoice_expire_at ON invoice(expire_at);
CREATE INDEX idx_payment_transaction_invoice_id ON payment_transaction(invoice_id);
CREATE INDEX idx_payment_transaction_reference_key ON payment_transaction(reference_key);
CREATE INDEX idx_payment_transaction_recipient_block_time ON payment_transaction(recipient_address, block_time);
CREATE INDEX idx_payment_proof_invoice_id ON payment_proof(invoice_id);
CREATE INDEX idx_payment_proof_tx_hash ON payment_proof(tx_hash);
CREATE INDEX idx_outbox_event_status_next_retry ON outbox_event(status, next_retry_at);
