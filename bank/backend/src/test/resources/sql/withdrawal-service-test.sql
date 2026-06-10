-- src/test/resources/sql/withdrawal-service-test.sql

-- 1. 성공 케이스용 고객 및 계좌 (ID: 100)
INSERT INTO customer (id, ci, customer_name, rrn_prefix, gender_code)
VALUES (100, 'withdraw-success-ci', '성공자', 'rrn-100', 1);
INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status)
VALUES (100, 100, 'acc-100', '$2a$12$ZpGgZxJgeb58kM7TE7hHb.V6gTNsBgXKknoVGaUy.WOrFkzVpfib.', 'DEPOSIT', 100000.00, 'NORMAL');

-- 2. 잔액 부족 테스트용 (ID: 101)
INSERT INTO customer (id, ci, customer_name, rrn_prefix, gender_code)
VALUES (101, 'withdraw-low-balance-ci', '가난뱅이', 'rrn-101', 2);
INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status)
VALUES (101, 101, 'acc-101', '$2a$12$ZpGgZxJgeb58kM7TE7hHb.V6gTNsBgXKknoVGaUy.WOrFkzVpfib.', 'DEPOSIT', 1000.00, 'NORMAL');

-- 3. 계좌 상태 비정상 테스트용 (ID: 102)
INSERT INTO customer (id, ci, customer_name, rrn_prefix, gender_code)
VALUES (102, 'withdraw-locked-ci', '잠긴자', 'rrn-102', 1);
INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status)
VALUES (102, 102, 'acc-102', '$2a$12$ZpGgZxJgeb58kM7TE7hHb.V6gTNsBgXKknoVGaUy.WOrFkzVpfib.', 'DEPOSIT', 50000.00, 'LOCKED');

-- 4. 잘못된 계좌 유형 테스트용 (ID: 103)
INSERT INTO customer (id, ci, customer_name, rrn_prefix, gender_code)
VALUES (103, 'withdraw-wrong-type-ci', '대출자', 'rrn-103', 2);
INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status)
VALUES (103, 103, 'acc-103', '$2a$12$ZpGgZxJgeb58kM7TE7hHb.V6gTNsBgXKknoVGaUy.WOrFkzVpfib.', 'LOAN', 50000.00, 'NORMAL');