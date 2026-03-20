-- Remove duplicated payment proofs before adding unique constraint / 添加唯一约束前清理重复支付凭证
DELETE FROM payment_proof a
USING payment_proof b
WHERE a.invoice_id = b.invoice_id
  AND a.tx_hash = b.tx_hash
  AND a.id < b.id;

-- Add unique constraint for invoice and transaction pair / 为账单与交易组合添加唯一约束
ALTER TABLE payment_proof
    ADD CONSTRAINT uk_payment_proof_invoice_tx UNIQUE (invoice_id, tx_hash);

COMMENT ON CONSTRAINT uk_payment_proof_invoice_tx ON payment_proof IS
    'Unique payment proof per invoice and transaction / 每张账单与交易唯一支付凭证约束';
