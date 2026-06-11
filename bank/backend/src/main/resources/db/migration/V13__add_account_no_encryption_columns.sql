ALTER TABLE account
    ADD COLUMN account_no_enc  VARCHAR(500) NULL COMMENT 'AES-256-GCM 암호화',
    ADD COLUMN account_no_hash VARCHAR(64)  NULL COMMENT 'SHA-256 Blind Index';
