-- ==========================================
-- bank_terms: terms_type 컬럼 추가
-- ==========================================
ALTER TABLE bank_terms
    ADD COLUMN terms_type VARCHAR(20) NOT NULL DEFAULT 'EVALUATION'
    COMMENT 'EVALUATION: 심사용 약관, CONTRACT: 계약용 약관';

-- ==========================================
-- 심사용 약관 시드 데이터
-- ==========================================
INSERT INTO bank_terms (terms_code, version, title, terms_url, is_mandatory, is_active, terms_type) VALUES
('CREDIT_INFO_AGREE', '1.0', '개인신용정보 수집·이용·제공 동의서',
 'https://cdn.woorifisan.com/terms/credit-info-agree-v1.html', true, true, 'EVALUATION'),
('NICE_CREDIT_INQUIRY', '1.0', 'NICE 신용정보 조회 동의서',
 'https://cdn.woorifisan.com/terms/nice-credit-inquiry-v1.html', true, true, 'EVALUATION'),
('DOCUMENT_COLLECT', '1.0', '서류 징구 및 보관 동의서',
 'https://cdn.woorifisan.com/terms/document-collect-v1.html', true, true, 'EVALUATION');

-- ==========================================
-- 계약용 약관 시드 데이터
-- ==========================================
INSERT INTO bank_terms (terms_code, version, title, terms_url, is_mandatory, is_active, terms_type) VALUES
('LOAN_CONTRACT_BASIC', '1.0', '대출 기본 약관',
 'https://cdn.woorifisan.com/terms/loan-contract-basic-v1.html', true, true, 'CONTRACT'),
('LOAN_PRODUCT_TERMS', '1.0', '대출 상품 설명서',
 'https://cdn.woorifisan.com/terms/loan-product-terms-v1.html', true, true, 'CONTRACT'),
('INTEREST_RATE_RISK', '1.0', '금리 변동 위험 고지서',
 'https://cdn.woorifisan.com/terms/interest-rate-risk-v1.html', false, true, 'CONTRACT'),
('EARLY_REPAYMENT_FEE', '1.0', '중도상환수수료 안내',
 'https://cdn.woorifisan.com/terms/early-repayment-fee-v1.html', false, true, 'CONTRACT');

-- ==========================================
-- 대출 상품 시드 데이터
-- ==========================================
INSERT INTO loan_product (product_name, min_rate, max_rate, min_limit, max_limit, conditions, status) VALUES
('우리 신용대출', 4.50, 12.00, 100000, 50000000,
 '신용점수 600점 이상, 재직기간 3개월 이상', 'ACTIVE_SALE'),
('우리 직장인 햇살론', 5.00, 15.00, 100000, 30000000,
 '신용점수 700점 이상, 재직기간 6개월 이상', 'ACTIVE_SALE'),
('우리 소호론', 6.00, 18.00, 500000, 100000000,
 '사업자등록 1년 이상, 신용점수 650점 이상', 'ACTIVE_SALE');
