ALTER TABLE loan_ledger
    ADD COLUMN approved_limit DECIMAL(18,2) COMMENT '심사 승인 한도 (실행 시 한도 초과 검증용)';
