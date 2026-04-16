ALTER TABLE merchant_payment_config
    ADD COLUMN ownership_verification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
    ADD COLUMN ownership_challenge_code VARCHAR(128),
    ADD COLUMN ownership_challenge_message VARCHAR(512),
    ADD COLUMN ownership_challenge_expires_at TIMESTAMPTZ,
    ADD COLUMN ownership_verification_signature TEXT,
    ADD COLUMN ownership_signature_submitted_at TIMESTAMPTZ,
    ADD COLUMN ownership_verified_at TIMESTAMPTZ;

COMMENT ON COLUMN merchant_payment_config.ownership_verification_status IS 'Wallet ownership verification status / 钱包地址所有权验证状态';
COMMENT ON COLUMN merchant_payment_config.ownership_challenge_code IS 'Latest wallet ownership challenge code / 最近一次钱包地址所有权挑战码';
COMMENT ON COLUMN merchant_payment_config.ownership_challenge_message IS 'Latest wallet ownership challenge message / 最近一次钱包地址所有权挑战消息';
COMMENT ON COLUMN merchant_payment_config.ownership_challenge_expires_at IS 'Latest wallet ownership challenge expiry time in UTC / 最近一次钱包地址所有权挑战过期时间（UTC）';
COMMENT ON COLUMN merchant_payment_config.ownership_verification_signature IS 'Latest submitted wallet ownership verification signature / 最近一次提交的钱包地址所有权验证签名';
COMMENT ON COLUMN merchant_payment_config.ownership_signature_submitted_at IS 'Latest wallet ownership signature submitted time in UTC / 最近一次钱包地址所有权签名提交时间（UTC）';
COMMENT ON COLUMN merchant_payment_config.ownership_verified_at IS 'Wallet ownership verified time in UTC / 钱包地址所有权验证完成时间（UTC）';
