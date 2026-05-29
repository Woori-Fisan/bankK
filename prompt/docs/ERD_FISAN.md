# FISAN ERD 문서

---

## 1. 중계 플랫폼 DB (platform_db)

### platform_admin (플랫폼 관리자)
모니터링 접근 가능자

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| login_id | VARCHAR(50) | UNIQUE | 운영자 로그인 ID |
| password_hash | VARCHAR(255) | | Bcrypt 단방향 해싱 |
| name | VARCHAR(50) | | 직원명 |
| is_active | BOOLEAN | | default: true |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### platform_user (플랫폼 사용자)
직원 사번, 로그인 ID 분리

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | 사용자 id |
| agency_id | BIGINT | FK(agency.id) | 소속 대행업체 |
| login_id | VARCHAR(50) | UNIQUE | 사번 등 로그인 ID |
| password_hash | VARCHAR(255) | | Bcrypt 단방향 해싱 |
| role | VARCHAR(20) | | AGENCY_USER(일반직원) / AGENCY_ADMIN(관리자) |
| failed_login_count | INT | | default: 0, 로그인 실패 횟수 |
| is_locked | BOOLEAN | | default: false, 5회 실패 시 계정 잠금 여부 |
| is_deleted | BOOLEAN | | default: false, 계정 삭제 여부 |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### agency (대행업체)
대행업체 별 담당 업무 종류 컬럼 추가. 우체국 같은 경우 대출 제외 시 접속 불가 처리.

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| agency_code | VARCHAR(10) | UNIQUE | 대행업체 코드 (예: PO001) |
| agency_name | VARCHAR(100) | | 업체명 (예: 우체국) |
| is_active | BOOLEAN | | default: true, 서비스 중단 시 FALSE |
| contract_start | DATE | | 계약 시작일 |
| contract_end | DATE | | 계약 종료일 |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### bank (은행)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| bank_code | VARCHAR(10) | UNIQUE | 금융결제원 표준 은행코드 |
| bank_name | VARCHAR(50) | | 제휴 은행명 |
| base_url | VARCHAR(255) | | BaaS API Base URL |
| is_active | BOOLEAN | | default: true, 서비스 중단 시 FALSE |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### bank_api_endpoint (은행 API 엔드포인트)
> ⚠️ DB에 저장해둘 필요가 있는지 팀 의사결정 필요 (현재 미적용)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| bank_id | BIGINT | FK(bank.id) | |
| api_type | VARCHAR(50) | | TRANSFER / BALANCE / LOAN |
| method | VARCHAR(10) | | POST / GET |
| endpoint_url | VARCHAR(200) | | 상세 엔드포인트 URI (예: /v1/transfer) |
| timeout_ms | INT | | 타임아웃 제한 시간(ms) |
| is_active | BOOLEAN | | default: true, 해당 API 호출 차단 시 FALSE |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

## 2. 은행 DB (bank_db)

### customer (고객정보)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| ci_hash | VARCHAR(255) | | 고객 식별 해시 (SHA-256) |
| customer_name | VARCHAR(100) | | 고객명 |
| rrn_prefix_enc | VARCHAR(255) | | 주민번호 앞 7자리 (AES-256 암호화) |
| gender_code | INT | | 성별 코드 |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### account (계좌정보)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | auto increment |
| customer_id | BIGINT | FK(customer.id) | |
| account_no_enc | VARCHAR(255) | | 계좌번호 원본 (AES-256 암호화) |
| account_no_hash | VARCHAR(255) | UNIQUE | 계좌 검색용 인덱스 (SHA-256) - Blind Index |
| password_hash | VARCHAR(255) | | RSA 구간 암호화(E2EE) 복호화 후 Bcrypt 해시 저장 |
| account_type | VARCHAR(50) | | 예금, 적금, 대출 등 |
| balance | DECIMAL(18,2) | | 현재 가용 잔액 |
| status | VARCHAR(20) | | NORMAL / LOCKED / CLOSED |
| version | INT | | default: 0, 비관적 락(Pessimistic Lock) 제어용 |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### transaction_ledger (거래원장정보)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| tx_id | VARCHAR(50) | PK | 거래 고유 ID |
| account_id | BIGINT | FK(account.id) | 거래 발생 계좌 ID |
| tx_type | VARCHAR(20) | | DEPOSIT / WITHDRAW / TRANSFER / LOAN |
| amount | DECIMAL(18,2) | | 거래 금액 |
| balance_after | DECIMAL(18,2) | | 거래 직후 잔액 스냅샷 |
| target_bank_code | VARCHAR(10) | | 타행 이체 시 상대 은행 코드 |
| target_account | VARCHAR(255) | | 타행 이체 시 상대 계좌번호 (AES-256 암호화) |
| status | VARCHAR(20) | | SUCCESS / FAILED / PENDING |
| transacted_at | DATETIME | | 거래일시 - 실제 거래 행위가 발생한 원천 시간 |
| created_at | DATETIME | | default: now() - DB INSERT 시간 |
| description | VARCHAR(100) | | 통장 인자 내용(적요) |

---

### loan_product (대출상품)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| product_id | BIGINT | PK | |
| product_name | VARCHAR(100) | | 상품명 |
| min_rate | DECIMAL(5,2) | | 최저금리 (%) |
| max_rate | DECIMAL(5,2) | | 최고금리 (%) |
| min_limit | DECIMAL(18,2) | | 최소한도 |
| max_limit | DECIMAL(18,2) | | 최대한도 |
| conditions | VARCHAR(500) | | 대상조건(텍스트 형태의 상품 안내 문구) |
| status | VARCHAR(20) | | ACTIVE_SALE / SUSPENDED |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### loan_review (대출심사)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| review_no | VARCHAR(50) | UNIQUE | 심사 번호 |
| customer_id | BIGINT | FK(customer.id) | |
| product_id | BIGINT | FK(loan_product.product_id) | |
| requested_amount | DECIMAL(18,2) | | 고객 신청 금액 |
| requested_period | INT | | 신청 상환 기간 (개월) |
| applied_credit_score | INT | | 심사 시점 신용 점수 |
| applied_dsr | DECIMAL(5,2) | | 심사 시점 DSR (%) |
| status | VARCHAR(20) | | SUBMITTED / UNDER_REVIEW / APPROVED / REJECTED / COMPLEMENT |
| reject_reason | VARCHAR(500) | | 거절 또는 보완 요청 사유 |
| created_at | DATETIME | | default: now(), 심사 요청 일시 |
| updated_at | DATETIME | | 최종 심사 처리 일시 |

---

### loan_ledger (대출원장정보)
> 심사 정보를 원장에 포함. 계약 번호는 심사 시작과 함께 생성.

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| loan_no | VARCHAR(50) | UNIQUE | 대출 계약 번호 |
| review_id | BIGINT | FK(loan_review.id) | 승인된 심사 건 연결 |
| product_id | BIGINT | FK(loan_product.product_id) | 가입한 대출 상품 ID |
| linked_account_id | BIGINT | FK(account.id) | 대출금 입금 및 원리금 출금 계좌 |
| loan_amount | DECIMAL(18,2) | | 최종 실행된 대출 원금 |
| current_loan_balance | DECIMAL(18,2) | | 현재 남은 대출 잔액 |
| interest_rate | DECIMAL(5,2) | | 확정 금리 (%) |
| repayment_type | VARCHAR(20) | | 원리금균등 / 원금균등 / 만기일시 |
| repayment_period | INT | | 상환 기간 (개월) |
| status | VARCHAR(20) | | ACTIVE / COMPLETED / DELINQUENT |
| start_date | DATETIME | | 대출 실행일 |
| end_date | DATETIME | | 대출 만기일 |
| created_at | DATETIME | | default: now() |
| updated_at | DATETIME | | |

---

### bank_terms (은행 약관)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| terms_code | VARCHAR(20) | | 약관 고유 코드 (예: T001) |
| version | VARCHAR(10) | | 버전 (예: v1.2) |
| title | VARCHAR(100) | | 약관명 (예: 개인정보 수집 동의서) |
| terms_url | VARCHAR(255) | | WebView로 띄워줄 정적 페이지(CDN) URL |
| is_mandatory | BOOLEAN | | 필수 동의 여부 |
| is_active | BOOLEAN | | default: true, 현재 서비스 중인 버전 여부 |
| created_at | DATETIME | | default: now() |

> **인덱스**: (terms_code, version) UNIQUE — 코드와 버전의 조합은 유일

---

### loan_review_terms_consent (약관 동의 이력)
> ⚠️ 멘토 피드백: 이 테이블 불필요. loan_review 테이블 내 Boolean 컬럼으로 대체. (미적용 / 참고용)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| review_id | BIGINT | FK(loan_review.id) | |
| terms_code | VARCHAR(20) | | 동의한 약관 코드 |
| agreed_version | VARCHAR(10) | | 고객이 실제로 본 약관 버전 (증적용) |
| is_agreed | BOOLEAN | | 동의 여부 |
| signature_data | TEXT | | Base64 서명 텍스트 또는 JWS 해시 데이터 |
| device_ip | VARCHAR(50) | | 동의 시점의 단말기 IP |
| agreed_at | DATETIME | | 단말기에서 실제 동의가 일어난 타임스탬프 |
| created_at | DATETIME | | default: now() |

---

### loan_review_document (파일 관리)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| review_id | BIGINT | FK(loan_review.id) | |
| document_type | VARCHAR(50) | | 서류 유형 (ID_CARD / INCOME_PROOF) |
| original_file_name | VARCHAR(255) | | 고객 업로드 원본 파일명 |
| file_path | VARCHAR(500) | | 온프레미스 스토리지 내 저장 경로 (예: /2026/05/12/uuid-1234.jpg) |
| created_at | DATETIME | | default: now() |

---

### bank_rsa_key_ledger (보안 및 키 관리)

| 컬럼명 | 자료형 | 키 | 비고 |
|--------|--------|-----|------|
| id | BIGINT | PK | |
| key_id | VARCHAR(50) | UNIQUE | 키 고유 식별자 (클라이언트가 헤더에 포함하여 보낼 Key Version) |
| public_key | TEXT | | 공개키 (PEM 또는 Base64 포맷, 클라이언트 배포용) |
| private_key_enc | TEXT | | 개인키 저장 (평문 저장 검토 중) |
| status | VARCHAR(20) | | ACTIVE / EXPIRED / REVOKED |
| valid_from | VARCHAR(20) | | 키 사용(배포) 시작 일시 |
| valid_to | VARCHAR(20) | | 키 사용(배포) 만료 일시 |
| created_at | DATETIME | | |
| updated_at | DATETIME | | |
