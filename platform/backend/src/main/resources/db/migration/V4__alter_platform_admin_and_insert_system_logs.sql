-- ==========================================
-- V4: platform_admin 테이블 구조 변경 및 시스템 로그 더미 데이터 삽입
-- 1. platform_admin 테이블에서 role 컬럼 제거
-- 2. system_logs 테스트용 더미 데이터 (4개 시나리오)
-- ==========================================

-- 1. 관리자 권한 체계 변경에 따른 컬럼 제거
ALTER TABLE platform_admin DROP COLUMN role;

-- 2. 시스템 로그 더미 데이터 삽입
-- (MySQL INTERVAL 문법에서는 MILLISECOND를 지원하지 않아 MICROSECOND 단위로 변환 적용)

-- [시나리오 1] 잔액 조회 (정상 흐름 - Trace ID: t1-balance-001)
INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, http_method, http_uri, http_status, elapsed_ms, client_ip, jws_signature, body_data)
VALUES (DATE_SUB(NOW(), INTERVAL 1 HOUR), 'INFO', 'CONTROLLER_REQ', 't1-balance-001', 'admin', 'PO001', '020', 'GET', '/api/v1/accounts/balance?accountNo=1002-123-456789', 200, 0, '192.168.1.10', 'eyJhbGciOiJSUzI1NiIsImtpZCI6InBvc3QtMSJ9...', '{"accountNo":"1002-123-456789"}');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 HOUR), INTERVAL 100000 MICROSECOND), 'INFO', 'BANK_REQ', 't1-balance-001', 'admin', 'PO001', '020', 'POST', 'https://baas.wooribank.com/api/v1/inquiry/balance', 200, 0, '127.0.0.1', 'ENC_DATA_REQUEST_V1_...BALANCE_INQUIRY');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 HOUR), INTERVAL 450000 MICROSECOND), 'INFO', 'BANK_RES', 't1-balance-001', 'admin', 'PO001', '020', 'POST', 'https://baas.wooribank.com/api/v1/inquiry/balance', 200, 350, '127.0.0.1', 'ENC_DATA_RESPONSE_V1_...BALANCE_AMOUNT_500000');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 HOUR), INTERVAL 500000 MICROSECOND), 'INFO', 'CONTROLLER_RES', 't1-balance-001', 'admin', 'PO001', '020', 'GET', '/api/v1/accounts/balance', 200, 500, '192.168.1.10', '{"status":"SUCCESS","data":{"balance":500000}}');


-- [시나리오 2] 계좌 이체 (정상 흐름 - Trace ID: t2-transfer-002)
INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, jws_signature, body_data)
VALUES (DATE_SUB(NOW(), INTERVAL 45 MINUTE), 'INFO', 'CONTROLLER_REQ', 't2-transfer-002', 'admin', 'PO001', '020', '088', 'POST', '/api/v1/transfers', 200, 0, '192.168.1.10', 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...', '{"from":"1002-123-456789","to":"110-456-789012","amount":10000}');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 45 MINUTE), INTERVAL 50000 MICROSECOND), 'INFO', 'BANK_REQ', 't2-transfer-002', 'admin', 'PO001', '020', '088', 'POST', 'https://baas.wooribank.com/api/v1/transfer', 200, 0, '127.0.0.1', 'ENC_DATA_TRANSFER_REQ_...10000');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 45 MINUTE), INTERVAL 1200000 MICROSECOND), 'INFO', 'BANK_RES', 't2-transfer-002', 'admin', 'PO001', '020', '088', 'POST', 'https://baas.wooribank.com/api/v1/transfer', 200, 1150, '127.0.0.1', 'ENC_DATA_TRANSFER_RES_...SUCCESS');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 45 MINUTE), INTERVAL 1250000 MICROSECOND), 'INFO', 'CONTROLLER_RES', 't2-transfer-002', 'admin', 'PO001', '020', '088', 'POST', '/api/v1/transfers', 200, 1250, '192.168.1.10', '{"status":"SUCCESS","message":"Transfer completed"}');


-- [시나리오 3] 이체 실패 - 잔액 부족 (Trace ID: t3-fail-balance-003)
INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, jws_signature, body_data)
VALUES (DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'INFO', 'CONTROLLER_REQ', 't3-fail-balance-003', 'admin', 'PO001', '020', '004', 'POST', '/api/v1/transfers', 400, 0, '192.168.1.10', 'eyJhbGciOiJSUzI1NiJ9...', '{"from":"1002-123-456789","to":"302-1234-5678-90","amount":999999999}');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 30 MINUTE), INTERVAL 50000 MICROSECOND), 'INFO', 'BANK_REQ', 't3-fail-balance-003', 'admin', 'PO001', '020', '004', 'POST', 'https://baas.wooribank.com/api/v1/transfer', 400, 0, '127.0.0.1', 'ENC_DATA_TRANSFER_REQ_...OVER_BALANCE');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, body_data, error_code, error_message)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 30 MINUTE), INTERVAL 300000 MICROSECOND), 'WARN', 'BANK_ERR', 't3-fail-balance-003', 'admin', 'PO001', '020', '004', 'POST', 'https://baas.wooribank.com/api/v1/transfer', 400, 250, '127.0.0.1', 'ENC_ERROR_RES_...INSUFFICIENT_FUNDS', 'B001', 'Insufficient balance');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, target_code, http_method, http_uri, http_status, elapsed_ms, client_ip, error_code, error_message)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 30 MINUTE), INTERVAL 350000 MICROSECOND), 'WARN', 'CONTROLLER_ERR', 't3-fail-balance-003', 'admin', 'PO001', '020', '004', 'POST', '/api/v1/transfers', 400, 350, '192.168.1.10', 'ERR_INSUFFICIENT_FUNDS', '잔액이 부족합니다.');


-- [시나리오 4] 은행 통신 오류 - 타임아웃 (Trace ID: t4-comm-err-004)
INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, http_method, http_uri, http_status, elapsed_ms, client_ip, jws_signature, body_data)
VALUES (DATE_SUB(NOW(), INTERVAL 10 MINUTE), 'INFO', 'CONTROLLER_REQ', 't4-comm-err-004', 'admin', 'PO001', '088', 'GET', '/api/v1/accounts/history?accountNo=110-456-789012', 500, 0, '192.168.1.10', '...', '{"accountNo":"110-456-789012"}');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, http_method, http_uri, http_status, elapsed_ms, client_ip, error_code, error_message)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 MINUTE), INTERVAL 5000000 MICROSECOND), 'ERROR', 'BANK_COMM_ERR', 't4-comm-err-004', 'admin', 'PO001', '088', 'POST', 'https://baas.shinhan.com/api/v1/inquiry/history', 504, 5000, '127.0.0.1', 'COMM_TIMEOUT', 'Bank API response timeout after 5000ms');

INSERT INTO system_logs (created_at, level, log_type, trace_id, staff_id, agency_code, bank_code, http_method, http_uri, http_status, elapsed_ms, client_ip, error_code, error_message)
VALUES (DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 MINUTE), INTERVAL 5050000 MICROSECOND), 'ERROR', 'CONTROLLER_ERR', 't4-comm-err-004', 'admin', 'PO001', '088', 'GET', '/api/v1/accounts/history', 500, 5050, '192.168.1.10', 'ERR_EXTERNAL_BANK_COMM', '은행 시스템과 통신 중 오류가 발생했습니다.');
