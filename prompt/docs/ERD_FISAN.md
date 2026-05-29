// ==========================================
// 1. [프로젝트 A] 중계 플랫폼 마스터 DB
// ==========================================
TableGroup Platform_DB {
platform_admin    // 플랫폼 직원
platform_user     // 사용자 (대행기관 직원)
agency            // 대행업체 (우체국 등)
bank              // 제휴 은행
bank_api_endpoint // 은행 BaaS API 엔드포인트
system_logs
}

Table platform_admin {
id bigint [primary key, increment]
login_id varchar(50) [unique, note: "운영자 로그인 ID"]
password_hash varchar(255) [note: "Bcrypt 해싱된 비밀번호"]
name varchar(50) [note: "직원명"]
role varchar(20) [note: "SUPER_ADMIN(전체), MONITOR(읽기전용), OPERATOR(운영자)"]
is_active boolean [default: true, note: "활성 상태"]
created_at timestamp [default: `now()`]
updated_at timestamp
}

Table platform_user {
id bigint [primary key, increment]
agency_id bigint [ref: > agency.id, note: "소속 대행업체 (FK)"]
agency_employee_num varchar(50) [note: "외부 사용자 사번"]
login_id varchar(50) [unique, note: "외부 사용자 ID"]
password_hash varchar(255) [note: "Bcrypt 해싱"]
role varchar(20) [note: "AGENCY_USER, AGENCY_ADMIN"] // 대행기관 내 권한
failed_login_count int [default: 0]
is_locked boolean [default: false]
is_deleted boolean [default: false]
created_at timestamp [default: `now()`]
updated_at timestamp
}

Table agency {
id bigint [primary key, increment]
agency_code varchar(10) [unique, note: "대행업체 코드 (예: PO001)"]
agency_name varchar(100) [note: "업체명 (예: 우체국)"]
allowed_services varchar(255) [note: "허용 금융 업무 종류 (예: DEPOSIT, TRANSFER, LOAN 등 메뉴 제어용)"]
is_active boolean [default: true, note: "서비스 상태"]
contract_start date
contract_end date
created_at timestamp [default: `now()`]
updated_at timestamp
}

Table bank {
id bigint [primary key, increment]
bank_code varchar(10) [unique, note: "금융결제원 표준 은행코드"]
bank_name varchar(50) [note: "제휴 은행명 (예: 국민은행)"]
base_url varchar(255) [note: "BaaS API Base URL"]
is_active boolean [default: true]
created_at timestamp [default: `now()`]
updated_at timestamp
}

Table bank_api_endpoint {
id bigint [primary key, increment]
bank_id bigint [ref: > bank.id]
api_type varchar(50) [note: "TRANSFER, BALANCE, LOAN 등"]
method varchar(10) [note: "POST, GET 등"]
endpoint_url varchar(200) [note: "상세 엔드포인트 URI (예: /v1/transfer)"]
timeout_ms int [note: "타임아웃 제한 시간(ms) - 서킷 브레이커용"]
is_active boolean [default: true]
created_at timestamp [default: `now()`]
updated_at timestamp
}

Table system_logs {
id            bigint        [primary key, increment]
created_at    datetime(3)   [not null, note: "로그 발생 시각 (밀리초 단위)"]
level         varchar(10)   [not null, note: "INFO / WARN / ERROR"]
log_type      varchar(20)   [not null, note: "CONTROLLER_REQ, CONTROLLER_RES, CONTROLLER_ERR, BANK_REQ, BANK_RES, BANK_ERR, BANK_COMM_ERR"]

trace_id      varchar(64)   [note: "단일 요청의 전체 흐름 추적 ID"]
staff_id      varchar(50)   [note: "요청한 직원 ID"]
agency_code   varchar(50)   [note: "대행기관 코드"]

bank_code     varchar(10)   [not null, note: "출금 은행 코드 (항상 존재)"]
target_code   varchar(10)   [note: "이체 시 입금 은행 코드 (이체 외 null)"]
bank_key_id   varchar(255)  [note: "RSA KEY ID (추후 구현, 현재 null)"]

http_method   varchar(10)   [note: "GET / POST"]
http_uri      varchar(255)  [note: "요청 URI"]
http_status   smallint      [note: "HTTP 응답 상태 코드"]
elapsed_ms    int           [note: "처리 시간 (밀리초)"]
client_ip     varchar(45)   [note: "요청 클라이언트 IP (IPv6 포함)"]

jws_signature text          [note: "JWS 전자서명 (부인방지 증거)"]
body_data     longtext      [note: "요청/응답 바디 전체 (암호화된 덩어리)"]

error_code    varchar(50)   [note: "커스텀 에러 코드 (예외 시만 채움)"]
error_message text          [note: "커스텀 에러 메시지 (예외 시만 채움)"]
}

// ==========================================
// 2. [프로젝트 B] 은행 코어 DB (Bank Core DB)
// ==========================================
TableGroup Bank_Core_DB {
customer
account
transaction_ledger
loan_product
loan_ledger        // 심사 및 대출 원장 통합
bank_terms         // 은행 약관 마스터
common_document    // [변경] 배타적 외래키 기반 통합 서류 관리
bank_rsa_key_ledger
}

Table customer {
id bigint [primary key, increment]
ci_hash varchar(255) [unique, note: "고객 식별 해시 (SHA-256)"]
customer_name varchar(100) [note: "고객명"]
rrn_prefix_enc varchar(255) [note: "주민번호 앞 7자리 (AES-256 암호화)"]
gender_code int [note: "성별 코드"]
created_at timestamp [default: `now()`]
updated_at timestamp
}

Table account {
id bigint [primary key, increment]
customer_id bigint [ref: > customer.id]
account_no_enc varchar(255) [note: "계좌번호 원본 (AES-256 암호화)"]
account_no_hash varchar(255) [unique, note: "계좌 검색용 인덱스 (SHA-256) - Blind Index"]
password_hash varchar(255) [note: "RSA 구간 암호화(E2EE) 복호화 후 Bcrypt 해시 저장"]
account_type varchar(50) [note: "예금(DEPOSIT), 적금(SAVING), 대출(LOAN)"]
balance decimal(18,2) [note: "현재 가용 잔액"]
status varchar(20) [note: "NORMAL, LOCKED, CLOSED"]
version int [default: 0, note: "낙관적 락(Optimistic Lock) 제어용"]
created_at timestamp [default: `now()`]
updated_at timestamp
}

Table transaction_ledger {
tx_id varchar(50) [primary key, note: "거래 고유 ID"]
account_id bigint [ref: > account.id, note: "거래 발생 계좌 ID"]
tx_type varchar(20) [note: "DEPOSIT, WITHDRAW, TRANSFER, LOAN"]
amount decimal(18,2) [note: "거래 금액"]
balance_after decimal(18,2) [note: "거래 직후 잔액 스냅샷"]
target_bank_code varchar(10) [note: "타행 이체 시 상대 은행 코드"]
target_account varchar(255) [note: "타행 이체 시 상대 계좌번호 (AES-256 암호화)"]
description varchar(100) [note: "통장 인자 내용(적요)"]  
status varchar(20) [note: "SUCCESS, FAILED, PENDING"]
transacted_at timestamp [note: "거래일시"]
created_at timestamp [default: `now()`]
}

Table loan_product {
product_id bigint [primary key, increment, note: "상품 ID"]
product_name varchar(100) [note: "상품명"]
min_rate decimal(5,2) [note: "최저금리 (%)"]
max_rate decimal(5,2) [note: "최고금리 (%)"]
min_limit decimal(18,2) [note: "최소한도"]
max_limit decimal(18,2) [note: "최대한도"]
conditions varchar(500) [note: "대상조건 (텍스트 또는 JSON 구조화)"]
status varchar(20) [note: "상태 (ACTIVE_SALE, SUSPENDED)"]
created_at timestamp [default: `now()`, note: "등록일시"]
updated_at timestamp [note: "변경일시"]
}

Table loan_ledger {
id bigint [primary key, increment]
loan_no varchar(50) [unique, note: "대출 계약 번호 (심사 시작과 동시에 발급)"]
customer_id bigint [ref: > customer.id]
product_id bigint [ref: > loan_product.product_id]
linked_account_id bigint [ref: > account.id, note: "대출금 입금 및 원리금 출금 계좌 (최종 실행 시 확정 연결)"]

// 심사 요청 정보
requested_amount decimal(18,2) [note: "고객 신청 금액"]
requested_period int [note: "신청 상환 기간 (개월)"]

// 통합 내재화된 약관 동의 및 서류 징구 플래그
is_credit_info_agreed boolean [default: false, note: "개인 신용 정보 이용 동의 여부 (Y/N)"]
is_product_terms_agreed boolean [default: false, note: "상품 약관 동의 여부 (Y/N)"]
is_document_collected boolean [default: false, note: "동의서/필수 서류 징구 완료 여부 (Y/N)"]

// 심사 결과 스냅샷
applied_credit_score int [note: "심사 시점 신용 점수"]
applied_dsr decimal(5,2) [note: "심사 시점 DSR (%)"]
reject_reason varchar(500) [note: "거절 또는 보완 요청 사유"]
reviewed_at timestamp [note: "심사 처리 완료 일시"]

// 대출 실행 원장 정보 (승인 및 최종 실행 완료 시 데이터 적재)
loan_amount decimal(18,2) [note: "최종 실행된 대출 원금"]
current_loan_balance decimal(18,2) [note: "현재 남은 대출 잔액"]
interest_rate decimal(5,2) [note: "확정 금리 (%)"]
repayment_type varchar(20) [note: "상환 방식 (원리금균등 등)"]
repayment_period int [note: "최종 확정 상환 기간 (개월)"]
start_date date [note: "대출 실행일"]
end_date date [note: "대출 만기일"]

// 통합 프로세스 라이프사이클 상태값
status varchar(30) [note: "SUBMITTED(신청), UNDER_REVIEW(심사중), APPROVED(승인), REJECTED(거절), COMPLEMENT(보완), ACTIVE(원장정상실행), COMPLETED(해지/완납), DELINQUENT(연체), EXPIRED(장기미실행만료/삭제대기), SYSTEM_ERROR(심사중 내부오류), WEBHOOK_FAILED(결과통보실패)"]

created_at timestamp [default: `now()`, note: "심사 최초 요청(원장 생성) 일시"]
updated_at timestamp [note: "최종 정보 변경 일시"]  
}

Table bank_terms {
id bigint [primary key, increment]
terms_code varchar(20) [note: "약관 고유 코드 (예: T001)"]
version varchar(10) [note: "버전 (예: v1.2)"]
title varchar(100) [note: "약관명 (예: 개인정보 수집 동의서)"]
terms_url varchar(255) [note: "WebView CDN URL"]
is_mandatory boolean [note: "필수 동의 여부"]
is_active boolean [default: true, note: "현재 서비스 버전 여부"]
created_at timestamp [default: `now()`]
indexes {
(terms_code, version) [unique]
}
}

// ------------------------------------------
// [최종 반영] 배타적 외래키 기반 통합 파일 관리 테이블
// ------------------------------------------
Table common_document {
id bigint [primary key, increment]

// 배타적 외래키 제약조건 (참조 무결성 보장, 무조건 둘 중 하나만 값 삽입)
loan_id bigint [ref: > loan_ledger.id, null, note: "대출 업무 서류일 때 해당 대출 ID 연결 (개인신용동의서 등)"]
account_id bigint [ref: > account.id, null, note: "예적금/신규 계좌 개설 관련 서류일 때 해당 계좌 ID 연결 (통장사본 등)"]

document_type varchar(50) [note: "서류 유형 (ID_CARD, INCOME_PROOF, REVENUE_AGREEMENT 등)"]
original_file_name varchar(255) [note: "고객 업로드 원본 파일명"]
file_path varchar(500) [note: "온프레미스 안전 스토리지 내 저장 경로"]
created_at timestamp [default: `now()`]
}

Table bank_rsa_key_ledger {
id bigint [primary key, increment]
key_id varchar(50) [unique, note: "키 고유 식별자 (Key Version)"]
public_key text [note: "공개키 (PEM 포맷, 클라이언트 배포용)"]
private_key_enc text [note: "개인키를 AES-256으로 암호화하여 안전하게 저장"]
status varchar(20) [note: "ACTIVE(현재 발급중), EXPIRED(과거 데이터 복호화용 만료키), REVOKED(유출폐기)"]
valid_from timestamp [note: "키 사용 시작 일시"]
valid_to timestamp [note: "키 사용 만료 일시"]
created_at timestamp [default: `now()`]
updated_at timestamp
}