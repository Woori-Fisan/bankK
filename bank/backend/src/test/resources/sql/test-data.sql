-- src/test/resources/sql/test-data.sql

INSERT INTO customer (ci_hash, customer_name, rrn_prefix_enc, gender_code)
VALUES (
           'test-ci-hash-sha256-dummy',
           '홍길동',
           'test-rrn-prefix-enc-aes256',
           1
       );

INSERT INTO account (customer_id, account_no_enc, account_no_hash, password_hash, account_type, balance, status, version)
VALUES (
           1,                                  -- customer_id (위에서 넣은 홍길동)
           'test-account-no-enc-aes256',       -- account_no_enc
           'test-account-no-hash-sha256',      -- account_no_hash
           'test-password-hash-bcrypt',        -- password_hash
           'CHECKING',                         -- account_type
           5000000.00,                         -- balance
           'NORMAL',                           -- status
           0                                   -- version
       );
