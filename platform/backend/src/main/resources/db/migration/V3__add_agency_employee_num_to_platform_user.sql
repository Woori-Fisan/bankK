-- ==========================================
-- V3: platform_user 테이블에 대행업체 직원 사번 컬럼 추가
-- ==========================================

-- 1. agency_employee_num 컬럼을 우선 NULL 허용으로 추가 (기존 데이터 대응)
ALTER TABLE platform_user ADD COLUMN agency_employee_num VARCHAR(50) NULL COMMENT '대행업체 직원 사번' AFTER login_id;

-- 2. 기존 데이터에 대해 임시 값 채우기 (여기서는 login_id를 기본값으로 활용)
UPDATE platform_user SET agency_employee_num = CONCAT('TEMP-', login_id) WHERE agency_employee_num IS NULL;

-- 3. NOT NULL 제약 조건 및 주석 적용
ALTER TABLE platform_user MODIFY COLUMN agency_employee_num VARCHAR(50) NOT NULL COMMENT '대행업체 직원 사번';
