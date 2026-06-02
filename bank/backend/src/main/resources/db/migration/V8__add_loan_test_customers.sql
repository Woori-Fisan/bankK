-- ==========================================
-- [테스트용] 대출 플로우 테스트 고객/계좌 시드 데이터
--
-- 팀원 테스트 정보:
--
-- [고객 1] 김민준
--   계좌번호: 1002100123456 / 비밀번호: 1234
--   이름: 김민준 / 생년월일: 910101 (1991-01-01)
--
-- [고객 2] 이수아
--   계좌번호: 1002100234567 / 비밀번호: 1234
--   이름: 이수아 / 생년월일: 880625 (1988-06-25)
--
-- [고객 3] 박지호
--   계좌번호: 1002100345678 / 비밀번호: 1234
--   이름: 박지호 / 생년월일: 950315 (1995-03-15)
--
-- DSR 안내: 신용점수 750점 고정, 연소득 3,600만원 기준 40% 한도
--   → 기존 대출 없는 상태이므로 신청 금액 2,000만원 이하 권장
-- ==========================================

-- ── customer ─────────────────────────────
INSERT IGNORE INTO customer (ci, customer_name, rrn_prefix, gender_code) VALUES
('TEST-CI-KIM',  '김민준', '9101011', 1),
('TEST-CI-LEE',  '이수아', '8806252', 2),
('TEST-CI-PARK', '박지호', '9503151', 1);

-- ── account ──────────────────────────────
-- bcrypt('1234') = $2a$12$GwBCpsHwYdRlqswfs984iOFXaI1Z9pQA20StavEnzWotysMORQLdy
INSERT IGNORE INTO account (customer_id, account_no, password, account_type, balance, status, version)
SELECT id, '1002100123456',
       '$2a$12$GwBCpsHwYdRlqswfs984iOFXaI1Z9pQA20StavEnzWotysMORQLdy',
       'CHECKING', 10000000.00, 'NORMAL', 0
FROM customer WHERE ci = 'TEST-CI-KIM';

INSERT IGNORE INTO account (customer_id, account_no, password, account_type, balance, status, version)
SELECT id, '1002100234567',
       '$2a$12$GwBCpsHwYdRlqswfs984iOFXaI1Z9pQA20StavEnzWotysMORQLdy',
       'CHECKING', 10000000.00, 'NORMAL', 0
FROM customer WHERE ci = 'TEST-CI-LEE';

INSERT IGNORE INTO account (customer_id, account_no, password, account_type, balance, status, version)
SELECT id, '1002100345678',
       '$2a$12$GwBCpsHwYdRlqswfs984iOFXaI1Z9pQA20StavEnzWotysMORQLdy',
       'CHECKING', 10000000.00, 'NORMAL', 0
FROM customer WHERE ci = 'TEST-CI-PARK';
