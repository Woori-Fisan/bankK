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
