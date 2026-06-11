-- src/test/resources/sql/test-data.sql

INSERT INTO customer (id, ci, customer_name, rrn_prefix, gender_code)
VALUES (
        1,
        'test-ci-plain',
        '홍길동',
        '900101',
        1
       );

INSERT INTO account (id, customer_id, account_no_enc, account_no_hash, password, account_type, balance, status)
VALUES (
        1,
        1,
        NULL,
        'jKlvdRBM+tcwkVsNfmT4WtAYp1Z8RvH/VMhITdlf5MA=',
        '$2a$10$ogtsOcJPvUgaIYq..uFjzurjS3fsp5/7nNOOMfC/DSZyvhO48x/bm', -- bcrypt '1234'
        'CHECKING',
        5000000.00,
        'NORMAL'
       );
