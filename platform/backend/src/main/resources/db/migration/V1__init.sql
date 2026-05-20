-- ==========================================
-- V1: 플랫폼 초기 테이블 생성
-- 생성 순서: agency → bank → platform_admin → platform_user (FK 의존성)
-- ==========================================

-- 1. 대행업체 테이블
CREATE TABLE agency
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '대행업체 PK',
    agency_code      VARCHAR(10)  NOT NULL                COMMENT '대행업체 코드 (예: PO001)',
    agency_name      VARCHAR(100) NOT NULL                COMMENT '업체명 (예: 우체국)',
    allowed_services VARCHAR(255)                         COMMENT '허용 금융 업무 종류 (예: DEPOSIT,TRANSFER,LOAN)',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE   COMMENT '서비스 활성 상태',
    contract_start   DATE                                 COMMENT '계약 시작일',
    contract_end     DATE                                 COMMENT '계약 종료일',
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()  COMMENT '생성일시',
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()  COMMENT '수정일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_agency_code (agency_code)
);

-- 2. 은행 테이블
CREATE TABLE bank
(
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '은행 PK',
    bank_code VARCHAR(10)  NOT NULL                COMMENT '금융결제원 표준 은행코드',
    bank_name VARCHAR(50)  NOT NULL                COMMENT '제휴 은행명 (예: 국민은행)',
    base_url  VARCHAR(255) NOT NULL                COMMENT 'BaaS API Base URL',
    is_active BOOLEAN      NOT NULL DEFAULT TRUE   COMMENT '활성 상태',
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()  COMMENT '생성일시',
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()  COMMENT '수정일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_bank_code (bank_code)
);

-- 3. 플랫폼 관리자 테이블
CREATE TABLE platform_admin
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '관리자 PK',
    login_id      VARCHAR(50)  NOT NULL                COMMENT '운영자 로그인 ID',
    password_hash VARCHAR(255) NOT NULL                COMMENT 'Bcrypt 해싱된 비밀번호',
    name          VARCHAR(50)  NOT NULL                COMMENT '직원명',
    role          VARCHAR(20)  NOT NULL                COMMENT '권한 (SUPER_ADMIN / MONITOR / OPERATOR)',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE   COMMENT '활성 상태',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()  COMMENT '생성일시',
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()  COMMENT '수정일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_platform_admin_login_id (login_id)
);

-- 4. 플랫폼 사용자 테이블 (agency FK 참조)
CREATE TABLE platform_user
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 PK',
    agency_id           BIGINT       NOT NULL                COMMENT '소속 대행업체 FK',
    login_id            VARCHAR(50)  NOT NULL                COMMENT '외부 사용자 로그인 ID',
    password_hash       VARCHAR(255) NOT NULL                COMMENT 'Bcrypt 해싱된 비밀번호',
    role                VARCHAR(30)  NOT NULL                COMMENT '권한 (AGENCY_USER / AGENCY_ADMIN)',
    failed_login_count  INT          NOT NULL DEFAULT 0      COMMENT '로그인 실패 횟수',
    is_locked           BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '계정 잠금 여부',
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '삭제 여부',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()  COMMENT '생성일시',
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()  COMMENT '수정일시',

    PRIMARY KEY (id),
    UNIQUE KEY uq_platform_user_login_id (login_id),
    CONSTRAINT fk_platform_user_agency FOREIGN KEY (agency_id) REFERENCES agency (id)
);

-- ==========================================
-- 초기 데이터 (Seed Data)
-- ==========================================

-- 1. 대행업체 초기 데이터
INSERT INTO agency (agency_code, agency_name, allowed_services, is_active, contract_start, contract_end)
VALUES ('PO001', '우체국',   'DEPOSIT,TRANSFER,LOAN', TRUE, '2025-01-01', '2027-12-31'),
       ('SB001', '저축은행', 'DEPOSIT,LOAN',           TRUE, '2025-01-01', '2027-12-31');

-- 2. 은행 초기 데이터 (금융결제원 표준 은행코드)
INSERT INTO bank (bank_code, bank_name, base_url, is_active)
VALUES ('020', '우리은행', 'https://baas.wooribank.com/api/v1', TRUE),
       ('088', '신한은행', 'https://baas.shinhan.com/api/v1',  TRUE),
       ('004', '국민은행', 'https://baas.kbstar.com/api/v1',    TRUE);

-- 3. 플랫폼 관리자 초기 데이터
-- password: 'password' BCrypt 해시
INSERT INTO platform_admin (login_id, password_hash, name, role, is_active)
VALUES ('admin', '$2b$10$SsrnGUcVMxglNJcZiuDdauyLsz.75fPpc.OaAu.59lPuYuvUB8m5W', '관리자', 'SUPER_ADMIN', TRUE);

-- 4. 플랫폼 사용자 초기 데이터 (agency_id=1: 우체국 소속)
-- password: 'password' BCrypt 해시
INSERT INTO platform_user (agency_id, login_id, password_hash, role, failed_login_count, is_locked, is_deleted)
VALUES (1, 'admin', '$2b$10$SsrnGUcVMxglNJcZiuDdauyLsz.75fPpc.OaAu.59lPuYuvUB8m5W', 'AGENCY_ADMIN', 0, FALSE, FALSE);