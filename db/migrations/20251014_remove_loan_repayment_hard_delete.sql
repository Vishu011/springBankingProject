-- HARD REMOVAL of legacy LOAN_REPAYMENT data and prevention of future inserts
-- Context: Remove loan-repayment completely from the platform.
-- DB: Oracle. Transaction entity maps EnumType.STRING to TRANSACTION_TYPE column in TRANSACTION table.

-- 1) Delete any legacy rows that still carry LOAN_REPAYMENT so JPA never touches them again.
DELETE FROM TRANSACTION
 WHERE TRANSACTION_TYPE = 'LOAN_REPAYMENT';

COMMIT;

-- 2) (Optional but recommended) Add a CHECK constraint to prevent reintroduction of unsupported types.
-- NOTE: If a constraint with the same name already exists, drop it first manually:
--   ALTER TABLE TRANSACTION DROP CONSTRAINT CK_TRANSACTION_TYPE;
-- Then run the below statement.
ALTER TABLE TRANSACTION
  ADD CONSTRAINT CK_TRANSACTION_TYPE
  CHECK (TRANSACTION_TYPE IN ('DEPOSIT','WITHDRAW','INTERNAL_DEBIT','TRANSFER','FINE'));

-- 3) Verification queries (run manually):
-- SELECT TRANSACTION_TYPE, COUNT(*) FROM TRANSACTION GROUP BY TRANSACTION_TYPE;
-- SELECT COUNT(*) AS CNT FROM TRANSACTION WHERE TRANSACTION_TYPE = 'LOAN_REPAYMENT';
-- SELECT CONSTRAINT_NAME, SEARCH_CONDITION FROM USER_CONSTRAINTS WHERE TABLE_NAME = 'TRANSACTION' AND CONSTRAINT_TYPE = 'C';
