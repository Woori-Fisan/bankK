-- src/test/resources/sql/account-service-test.sql

INSERT INTO customer (id, ci, customer_name, rrn_prefix, gender_code)
VALUES (
           10,
           'service-test-ci',
           '테스터',
           '9501014',
           1
       );

INSERT INTO account (id, customer_id, account_no, password, account_type, balance, status, version)
VALUES (
           10,
           10,
           '111-222-3333',
           'service-test-pw',
           'SAVINGS',
           150000.00,
           'NORMAL',
           0
       );

INSERT INTO transaction_ledger (tx_id, account_id, tx_type, amount, balance_after, target_bank_code, target_account, description, status, transacted_at)
VALUES (
           (1, 10, 'DEPOSIT', 100000.00, 100000.00, NULL, NULL, '급여', 'SUCCESS', '2026-05-01 09:00:00'),
           (2, 10, 'WITHDRAWAL', 15000.00, 85000.00, NULL, NULL, '점심식사', 'SUCCESS', '2026-05-02 12:30:00'),
           (3, 10, 'TRANSFER_OUT', 30000.00, 55000.00, '004', '111-222-333', '친구송금', 'SUCCESS', '2026-05-03 18:45:00'),
           (4, 10, 'TRANSFER_IN', 50000.00, 105000.00, '081', '444-555-666', '용돈', 'SUCCESS', '2026-05-05 10:20:00'),
           (5, 10, 'WITHDRAWAL', 5000.00, 100000.00, NULL, NULL, '커피', 'SUCCESS', '2026-05-06 13:10:00'),
           (6, 10, 'DEPOSIT', 200000.00, 300000.00, NULL, NULL, '상여금', 'SUCCESS', '2026-05-08 09:00:00'),
           (7, 10, 'TRANSFER_OUT', 100000.00, 200000.00, '020', '777-888-999', '적금', 'SUCCESS', '2026-05-08 10:05:00'),
           (8, 10, 'WITHDRAWAL', 12000.00, 188000.00, NULL, NULL, '저녁식사', 'SUCCESS', '2026-05-09 19:30:00'),
           (9, 10, 'WITHDRAWAL', 8000.00, 180000.00, NULL, NULL, '편의점', 'SUCCESS', '2026-05-10 21:15:00'),
           (10, 10, 'TRANSFER_OUT', 50000.00, 130000.00, '088', '123-123-123', '공과금', 'SUCCESS', '2026-05-11 14:00:00'),
           (11, 10, 'DEPOSIT', 70000.00, 200000.00, NULL, NULL, '중고거래', 'SUCCESS', '2026-05-12 16:40:00'),
           (12, 10, 'WITHDRAWAL', 25000.00, 175000.00, NULL, NULL, '쇼핑', 'SUCCESS', '2026-05-14 15:20:00'),
           (13, 10, 'TRANSFER_IN', 15000.00, 190000.00, '004', '999-888-777', '더치페이 입금', 'SUCCESS', '2026-05-15 12:50:00'),
           (14, 10, 'WITHDRAWAL', 10000.00, 180000.00, NULL, NULL, '교통비', 'SUCCESS', '2026-05-16 08:30:00'),
           (15, 10, 'TRANSFER_OUT', 40000.00, 140000.00, '004', '555-444-333', '통신비', 'SUCCESS', '2026-05-18 10:00:00'),
           (16, 10, 'WITHDRAWAL', 20000.00, 120000.00, NULL, NULL, '영화관', 'SUCCESS', '2026-05-20 19:10:00'),
           (17, 10, 'DEPOSIT', 60000.00, 180000.00, NULL, NULL, '환불금', 'SUCCESS', '2026-05-21 11:25:00'),
           (18, 10, 'WITHDRAWAL', 18000.00, 162000.00, NULL, NULL, '도서구매', 'SUCCESS', '2026-05-22 14:45:00'),
           (19, 10, 'TRANSFER_OUT', 32000.00, 130000.00, '020', '333-222-111', '구독료', 'SUCCESS', '2026-05-24 09:30:00'),
           (20, 10, 'TRANSFER_IN', 20000.00, 150000.00, '081', '777-666-555', '캐시백', 'SUCCESS', '2026-05-25 15:00:00');
      );
