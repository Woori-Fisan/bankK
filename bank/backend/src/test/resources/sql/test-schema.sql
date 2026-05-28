-- src/test/resources/sql/test-schema.sql

-- ==========================================
-- 고객
-- ==========================================
CREATE TABLE IF NOT EXISTS customer (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ci              VARCHAR(255) NOT NULL UNIQUE,
    customer_name   VARCHAR(100) NOT NULL,
    rrn_prefix      VARCHAR(20)  NOT NULL,
    gender_code     INT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- ==========================================
-- 계좌
-- ==========================================
CREATE TABLE IF NOT EXISTS account (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT NOT NULL,
    account_no       VARCHAR(30)  NOT NULL,
    password         VARCHAR(255) NOT NULL,
    account_type     VARCHAR(50)  NOT NULL,
    balance          DECIMAL(18,2) NOT NULL DEFAULT 0,
    status           VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    version          INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- ==========================================
-- 거래 원장
-- ==========================================
CREATE TABLE IF NOT EXISTS transaction_ledger (
    tx_id            VARCHAR(50)   PRIMARY KEY,
    account_id       BIGINT        NOT NULL,
    tx_type          VARCHAR(20)   NOT NULL,
    amount           DECIMAL(18,2) NOT NULL,
    balance_after    DECIMAL(18,2) NOT NULL,
    target_bank_code VARCHAR(10),
    target_account   VARCHAR(255),
    description      VARCHAR(100),
    status           VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    transacted_at    TIMESTAMP     NOT NULL,
    created_at       TIMESTAMP     DEFAULT NOW(),
    CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES account(id)
);

-- ==========================================
-- 대출 상품
-- ==========================================
CREATE TABLE IF NOT EXISTS loan_product (
    product_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100)  NOT NULL,
    min_rate     DECIMAL(5,2)  NOT NULL,
    max_rate     DECIMAL(5,2)  NOT NULL,
    min_limit    DECIMAL(18,2) NOT NULL,
    max_limit    DECIMAL(18,2) NOT NULL,
    conditions   VARCHAR(500),
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE_SALE',
    created_at   TIMESTAMP     DEFAULT NOW(),
    updated_at   TIMESTAMP     DEFAULT NOW()
);

-- ==========================================
-- 대출 원장
-- ==========================================
CREATE TABLE IF NOT EXISTS loan_ledger (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_no                 VARCHAR(50)   NOT NULL UNIQUE,
    customer_id             BIGINT        NOT NULL,
    product_id              BIGINT,
    linked_account_id       BIGINT,
    requested_amount        DECIMAL(18,2) NOT NULL,
    requested_period        INT           NOT NULL,
    is_credit_info_agreed   BOOLEAN       NOT NULL DEFAULT FALSE,
    is_product_terms_agreed BOOLEAN       NOT NULL DEFAULT FALSE,
    is_document_collected   BOOLEAN       NOT NULL DEFAULT FALSE,
    applied_credit_score    INT,
    applied_dsr             DECIMAL(5,2),
    approved_limit          DECIMAL(18,2),
    reject_reason           VARCHAR(500),
    reviewed_at             TIMESTAMP,
    loan_amount             DECIMAL(18,2),
    current_loan_balance    DECIMAL(18,2),
    interest_rate           DECIMAL(5,2),
    repayment_type          VARCHAR(20),
    repayment_period        INT,
    start_date              DATE,
    end_date                DATE,
    status                  VARCHAR(30)   NOT NULL DEFAULT 'SUBMITTED',
    created_at              TIMESTAMP     DEFAULT NOW(),
    updated_at              TIMESTAMP     DEFAULT NOW(),
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id)       REFERENCES customer(id),
    CONSTRAINT fk_loan_product  FOREIGN KEY (product_id)        REFERENCES loan_product(product_id),
    CONSTRAINT fk_loan_account  FOREIGN KEY (linked_account_id) REFERENCES account(id)
);

-- ==========================================
-- 은행 약관
-- ==========================================
CREATE TABLE IF NOT EXISTS bank_terms (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    terms_code   VARCHAR(20)  NOT NULL,
    version      VARCHAR(10)  NOT NULL,
    title        VARCHAR(100) NOT NULL,
    terms_url    VARCHAR(255),
    is_mandatory BOOLEAN      NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    terms_type    VARCHAR(20)  NOT NULL DEFAULT 'EVALUATION',
    terms_content LONGTEXT,
    created_at    TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_terms UNIQUE (terms_code, version)
);

-- ==========================================
-- RSA 키 원장
-- ==========================================
CREATE TABLE IF NOT EXISTS bank_rsa_key_ledger (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_id          VARCHAR(50) NOT NULL UNIQUE,
    public_key      TEXT        NOT NULL,
    private_key_enc TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    valid_from      TIMESTAMP   NOT NULL,
    valid_to        TIMESTAMP   NOT NULL,
    created_at      TIMESTAMP   DEFAULT NOW(),
    updated_at      TIMESTAMP   DEFAULT NOW()
);
