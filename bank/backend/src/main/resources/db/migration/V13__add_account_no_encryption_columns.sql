-- 계좌번호 암호화를 위한 컬럼 추가
-- account_no_enc: AES-256-GCM 방식으로 암호화된 계좌번호 저장
-- account_no_hash: SHA-256 방식으로 해싱된 계좌번호 저장 (Blind Index 용도)
ALTER TABLE account
    ADD COLUMN account_no_enc  VARCHAR(500) NULL COMMENT 'AES-256-GCM 암호화',
    ADD COLUMN account_no_hash VARCHAR(64)  NULL COMMENT 'SHA-256 Blind Index';
