-- account 테이블: 암호화 컬럼 제거·이름 변경
ALTER TABLE account
    CHANGE account_no_enc account_no VARCHAR(30)  NOT NULL,
    DROP   COLUMN account_no_hash,
    CHANGE password_hash  password   VARCHAR(255) NOT NULL;

-- customer 테이블: 암호화 컬럼 이름 변경
ALTER TABLE customer
    CHANGE ci_hash        ci         VARCHAR(255) NOT NULL,
    CHANGE rrn_prefix_enc rrn_prefix VARCHAR(20)  NOT NULL;
