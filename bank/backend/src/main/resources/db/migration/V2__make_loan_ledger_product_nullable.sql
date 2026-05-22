-- loan_ledger.product_id: 심사 시점에는 상품 미선택 → NULL 허용
ALTER TABLE loan_ledger
    MODIFY COLUMN product_id BIGINT NULL COMMENT 'loan_product FK - 실행 시 확정';
