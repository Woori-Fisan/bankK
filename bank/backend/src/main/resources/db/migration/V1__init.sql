-- ==========================================
-- 고객
-- ==========================================
CREATE TABLE IF NOT EXISTS customer (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ci_hash         VARCHAR(255) NOT NULL UNIQUE COMMENT '고객 식별 해시 SHA-256',
    customer_name   VARCHAR(100) NOT NULL COMMENT '고객명',
    rrn_prefix_enc  VARCHAR(255) NOT NULL COMMENT '주민번호 앞 7자리 AES-256 암호화',
    gender_code     INT COMMENT '성별 코드',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW() ON UPDATE NOW()
);

-- ==========================================
-- 계좌
-- ==========================================
CREATE TABLE IF NOT EXISTS account (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT NOT NULL COMMENT 'customer FK',
    account_no_enc   VARCHAR(255) NOT NULL COMMENT '계좌번호 AES-256 암호화',
    account_no_hash  VARCHAR(255) NOT NULL UNIQUE COMMENT '계좌 검색용 Blind Index SHA-256',
    password_hash    VARCHAR(255) NOT NULL COMMENT 'E2EE 복호화 후 bcrypt 해시',
    account_type     VARCHAR(50)  NOT NULL COMMENT '예금, 적금, 대출 등',
    balance          DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '현재 가용 잔액',
    status           VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL, LOCKED, CLOSED',
    version          INT NOT NULL DEFAULT 0 COMMENT '낙관적 락 제어용',
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW() ON UPDATE NOW(),
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- ==========================================
-- 거래 원장
-- ==========================================
CREATE TABLE IF NOT EXISTS transaction_ledger (
    tx_id            VARCHAR(50)   PRIMARY KEY COMMENT '거래 고유 ID',
    account_id       BIGINT        NOT NULL COMMENT '거래 발생 계좌 FK',
    tx_type          VARCHAR(20)   NOT NULL COMMENT 'DEPOSIT, WITHDRAW, TRANSFER, LOAN',
    amount           DECIMAL(18,2) NOT NULL COMMENT '거래 금액',
    balance_after    DECIMAL(18,2) NOT NULL COMMENT '거래 직후 잔액 스냅샷',
    target_bank_code VARCHAR(10)   COMMENT '타행 이체 시 상대 은행 코드',
    target_account   VARCHAR(255)  COMMENT '타행 이체 시 상대 계좌번호 AES-256 암호화',
    description      VARCHAR(100)  COMMENT '통장 인자 내용 적요',
    status           VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS, FAILED, PENDING',
    transacted_at    TIMESTAMP     NOT NULL COMMENT '거래일시',
    created_at       TIMESTAMP     DEFAULT NOW(),
    CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES account(id)
);

-- ==========================================
-- 대출 상품
-- ==========================================
CREATE TABLE IF NOT EXISTS loan_product (
    product_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 ID',
    product_name VARCHAR(100)  NOT NULL COMMENT '상품명',
    min_rate     DECIMAL(5,2)  NOT NULL COMMENT '최저금리 %',
    max_rate     DECIMAL(5,2)  NOT NULL COMMENT '최고금리 %',
    min_limit    DECIMAL(18,2) NOT NULL COMMENT '최소한도',
    max_limit    DECIMAL(18,2) NOT NULL COMMENT '최대한도',
    conditions   VARCHAR(500)  COMMENT '대상조건',
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE_SALE' COMMENT 'ACTIVE_SALE, SUSPENDED',
    created_at   TIMESTAMP     DEFAULT NOW(),
    updated_at   TIMESTAMP     DEFAULT NOW() ON UPDATE NOW()
);

-- ==========================================
-- 대출 원장 (심사 + 실행 통합)
-- ==========================================
CREATE TABLE IF NOT EXISTS loan_ledger (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_no                 VARCHAR(50)   NOT NULL UNIQUE COMMENT '대출 계약 번호',
    customer_id             BIGINT        NOT NULL COMMENT 'customer FK',
    product_id              BIGINT        NOT NULL COMMENT 'loan_product FK',
    linked_account_id       BIGINT        COMMENT '대출금 입금 계좌 FK 최종 실행 시 확정',
    -- 심사 요청 정보
    requested_amount        DECIMAL(18,2) NOT NULL COMMENT '고객 신청 금액',
    requested_period        INT           NOT NULL COMMENT '신청 상환 기간 개월',
    -- 약관 동의 및 서류 징구 플래그
    is_credit_info_agreed   BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '개인 신용 정보 이용 동의',
    is_product_terms_agreed BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '상품 약관 동의',
    is_document_collected   BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '서류 징구 완료',
    -- 심사 결과 스냅샷
    applied_credit_score    INT           COMMENT '심사 시점 신용 점수',
    applied_dsr             DECIMAL(5,2)  COMMENT '심사 시점 DSR %',
    reject_reason           VARCHAR(500)  COMMENT '거절 또는 보완 요청 사유',
    reviewed_at             TIMESTAMP     COMMENT '심사 처리 완료 일시',
    -- 대출 실행 원장 (승인 후 적재)
    loan_amount             DECIMAL(18,2) COMMENT '최종 실행된 대출 원금',
    current_loan_balance    DECIMAL(18,2) COMMENT '현재 남은 대출 잔액',
    interest_rate           DECIMAL(5,2)  COMMENT '확정 금리 %',
    repayment_type          VARCHAR(20)   COMMENT '상환 방식',
    repayment_period        INT           COMMENT '최종 확정 상환 기간 개월',
    start_date              DATE          COMMENT '대출 실행일',
    end_date                DATE          COMMENT '대출 만기일',
    -- 라이프사이클 상태
    status                  VARCHAR(30)   NOT NULL DEFAULT 'SUBMITTED'
    COMMENT 'SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, COMPLEMENT, ACTIVE, COMPLETED, DELINQUENT, EXPIRED',
    created_at              TIMESTAMP     DEFAULT NOW(),
    updated_at              TIMESTAMP     DEFAULT NOW() ON UPDATE NOW(),
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id)       REFERENCES customer(id),
    CONSTRAINT fk_loan_product  FOREIGN KEY (product_id)        REFERENCES loan_product(product_id),
    CONSTRAINT fk_loan_account  FOREIGN KEY (linked_account_id) REFERENCES account(id)
);

-- ==========================================
-- 은행 약관
-- ==========================================
CREATE TABLE IF NOT EXISTS bank_terms (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    terms_code   VARCHAR(20)  NOT NULL COMMENT '약관 고유 코드',
    version      VARCHAR(10)  NOT NULL COMMENT '버전',
    title        VARCHAR(100) NOT NULL COMMENT '약관명',
    terms_url    VARCHAR(255) COMMENT 'WebView CDN URL',
    is_mandatory BOOLEAN      NOT NULL COMMENT '필수 동의 여부',
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '현재 서비스 버전 여부',
    created_at   TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_terms UNIQUE (terms_code, version)
);

-- ==========================================
-- 통합 서류 관리 (배타적 외래키)
-- ==========================================
CREATE TABLE IF NOT EXISTS common_document (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id            BIGINT       COMMENT '대출 업무 서류일 때 loan_ledger FK',
    account_id         BIGINT       COMMENT '계좌 개설 서류일 때 account FK',
    document_type      VARCHAR(50)  NOT NULL COMMENT 'ID_CARD, INCOME_PROOF, REVENUE_AGREEMENT 등',
    original_file_name VARCHAR(255) NOT NULL COMMENT '고객 업로드 원본 파일명',
    file_path          VARCHAR(500) NOT NULL COMMENT '온프레미스 스토리지 저장 경로',
    created_at         TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT fk_doc_loan    FOREIGN KEY (loan_id)    REFERENCES loan_ledger(id),
    CONSTRAINT fk_doc_account FOREIGN KEY (account_id) REFERENCES account(id),
    -- 배타적 외래키: 둘 중 하나만 NOT NULL
    CONSTRAINT chk_doc_exclusive CHECK (
        (loan_id IS NOT NULL AND account_id IS NULL) OR (loan_id IS NULL AND account_id IS NOT NULL)
    )
);

-- ==========================================
-- RSA 키 원장
-- ==========================================
CREATE TABLE IF NOT EXISTS bank_rsa_key_ledger (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_id          VARCHAR(50) NOT NULL UNIQUE COMMENT '키 고유 식별자 Key Version',
    public_key      TEXT        NOT NULL COMMENT '공개키 PEM 포맷',
    private_key_enc TEXT        NOT NULL COMMENT '개인키 AES-256 암호화 저장',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, EXPIRED, REVOKED',
    valid_from      TIMESTAMP   NOT NULL COMMENT '키 사용 시작 일시',
    valid_to        TIMESTAMP   NOT NULL COMMENT '키 사용 만료 일시',
    created_at      TIMESTAMP   DEFAULT NOW(),
    updated_at      TIMESTAMP   DEFAULT NOW() ON UPDATE NOW()
);