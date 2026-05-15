-- =====================================================
-- BankBridge 중계 플랫폼 DB (platform_db) DDL
-- =====================================================

CREATE DATABASE IF NOT EXISTS platform_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE platform_db;

-- ── 대행업체 ──────────────────────────────────────────
CREATE TABLE agency
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    agency_code    VARCHAR(10)  NOT NULL COMMENT '대행업체 코드 (예: PO001)',
    agency_name    VARCHAR(100) NOT NULL COMMENT '업체명 (예: 우체국)',
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '서비스 중단 시 FALSE',
    contract_start DATE         NOT NULL COMMENT '계약 시작일',
    contract_end   DATE         NOT NULL COMMENT '계약 종료일',
    created_at     DATETIME     NOT NULL DEFAULT NOW(),
    updated_at     DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uq_agency_code (agency_code)
) COMMENT '대행업체';

-- ── 플랫폼 사용자 ─────────────────────────────────────
CREATE TABLE platform_user
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    agency_id           BIGINT       NOT NULL COMMENT '소속 대행업체 FK',
    login_id            VARCHAR(50)  NOT NULL COMMENT '사번 등 로그인 ID',
    password_hash       VARCHAR(255) NOT NULL COMMENT 'Bcrypt 단방향 해싱',
    role                VARCHAR(20)  NOT NULL COMMENT 'AGENCY_USER / AGENCY_ADMIN',
    failed_login_count  INT          NOT NULL DEFAULT 0 COMMENT '로그인 실패 횟수',
    is_locked           BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '5회 실패 시 계정 잠금',
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '계정 삭제 여부',
    created_at          DATETIME     NOT NULL DEFAULT NOW(),
    updated_at          DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uq_platform_user_login_id (login_id),
    CONSTRAINT fk_platform_user_agency FOREIGN KEY (agency_id) REFERENCES agency (id)
) COMMENT '플랫폼 사용자';

-- ── 플랫폼 관리자 ─────────────────────────────────────
CREATE TABLE platform_admin
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    login_id      VARCHAR(50)  NOT NULL COMMENT '운영자 로그인 ID',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Bcrypt 단방향 해싱',
    name          VARCHAR(50)  NOT NULL COMMENT '직원명',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    updated_at    DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uq_platform_admin_login_id (login_id)
) COMMENT '플랫폼 관리자';

-- ── 은행 ──────────────────────────────────────────────
CREATE TABLE bank
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    bank_code  VARCHAR(10)  NOT NULL COMMENT '금융결제원 표준 은행코드',
    bank_name  VARCHAR(50)  NOT NULL COMMENT '제휴 은행명',
    base_url   VARCHAR(255) NOT NULL COMMENT 'BaaS API Base URL',
    public_key TEXT COMMENT 'RSA 공개키 (JWKS 배치로 갱신)',
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '서비스 중단 시 FALSE',
    created_at DATETIME     NOT NULL DEFAULT NOW(),
    updated_at DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uq_bank_code (bank_code)
) COMMENT '제휴 은행';

-- ── 은행 API 엔드포인트 ───────────────────────────────
CREATE TABLE bank_api_endpoint
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    bank_id      BIGINT       NOT NULL COMMENT 'FK(bank.id)',
    api_type     VARCHAR(50)  NOT NULL COMMENT 'TRANSFER / BALANCE / LOAN',
    method       VARCHAR(10)  NOT NULL COMMENT 'POST / GET',
    endpoint_url VARCHAR(200) NOT NULL COMMENT '상세 엔드포인트 URI (예: /v1/transfer)',
    timeout_ms   INT          NOT NULL COMMENT '타임아웃 제한 시간(ms)',
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '해당 API 호출 차단 시 FALSE',
    created_at   DATETIME     NOT NULL DEFAULT NOW(),
    updated_at   DATETIME     NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_bank_api_endpoint_bank FOREIGN KEY (bank_id) REFERENCES bank (id)
) COMMENT '은행 API 엔드포인트';