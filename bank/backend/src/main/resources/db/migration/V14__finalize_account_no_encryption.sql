-- 계좌번호 암호화 마이그레이션 완료 후 처리
-- 1. 암호화 및 해시 컬럼을 NOT NULL로 변경
-- 2. 기존 평문 계좌번호(account_no) 컬럼 삭제
-- 3. 해시 컬럼(Blind Index)에 고유 인덱스 추가하여 조회 성능 확보
ALTER TABLE account
    MODIFY COLUMN account_no_enc  VARCHAR(500) NOT NULL,
    MODIFY COLUMN account_no_hash VARCHAR(64)  NOT NULL,
    DROP COLUMN account_no;

ALTER TABLE account ADD UNIQUE INDEX idx_account_no_hash (account_no_hash);
