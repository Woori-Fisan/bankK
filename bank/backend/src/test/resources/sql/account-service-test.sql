-- src/test/resources/sql/account-service-test.sql

-- 테스트를 위한 기초 데이터 (기존 데이터와 충돌 방지를 위해 다른 값 사용)
INSERT INTO customer (id, ci_hash, customer_name, rrn_prefix_enc, gender_code)
VALUES (
           10,
           'service-test-ci-hash',
           '테스터',
           'service-test-rrn-prefix',
           1
       );

INSERT INTO account (id, customer_id, account_no_enc, account_no_hash, password_hash, account_type, balance, status, version)
VALUES (
           10,
           10,
           'service-test-acc-enc',
           'service-test-acc-hash',
           'service-test-pw-hash',
           'SAVINGS',
           150000.00,
           'NORMAL',
           0
       );
