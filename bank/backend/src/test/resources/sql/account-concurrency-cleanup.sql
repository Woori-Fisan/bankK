-- FK 순서에 맞게 역순으로 삭제
DELETE FROM transaction_ledger WHERE account_id IN (201, 202, 203);
DELETE FROM account WHERE id IN (201, 202, 203);
DELETE FROM customer WHERE id = 200;