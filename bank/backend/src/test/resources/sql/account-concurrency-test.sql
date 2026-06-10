-- 동시성 테스트용 고객 (ID: 200)
INSERT INTO customer (id, ci, customer_name, rrn_prefix, gender_code)
VALUES (200, 'concurrency-test-ci', '동시성테스터', 'rrn-200', 1);

-- 계좌 201: 10,000원 (더블 스펜딩 테스트 — 두 스레드가 각각 10,000원 출금 시도)
INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status)
VALUES (201, 200, 'acc-201', '$2a$12$ZpGgZxJgeb58kM7TE7hHb.V6gTNsBgXKknoVGaUy.WOrFkzVpfib.', 'DEPOSIT', 10000.00, 'NORMAL');

-- 계좌 202: 100,000원 (N개 스레드 Lost Update 테스트 — 10개 스레드가 10,000원씩 동시 출금)
INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status)
VALUES (202, 200, 'acc-202', '$2a$12$ZpGgZxJgeb58kM7TE7hHb.V6gTNsBgXKknoVGaUy.WOrFkzVpfib.', 'DEPOSIT', 100000.00, 'NORMAL');

-- 계좌 203: 10,000원 (락 없는 경쟁 조건 재현용 — 락 필요성 입증)
INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status)
VALUES (203, 200, 'acc-203', '$2a$12$ZpGgZxJgeb58kM7TE7hHb.V6gTNsBgXKknoVGaUy.WOrFkzVpfib.', 'DEPOSIT', 10000.00, 'NORMAL');