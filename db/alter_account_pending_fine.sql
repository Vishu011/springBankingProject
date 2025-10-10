-- Add missing pending_fine_amount column required by AccountMicroservice
-- Run as ACCOUNT_MS (or the schema that owns the ACCOUNT table)
-- Example:
--   sqlplus ACCOUNT_MS/account123@localhost:1521/ORCL @db/alter_account_pending_fine.sql
--
-- Verify current columns (optional):
--   SELECT column_name, data_type, data_length, data_precision, data_scale, nullable
--   FROM user_tab_columns
--   WHERE table_name = 'ACCOUNT'
--   ORDER BY column_id;

ALTER TABLE account ADD (pending_fine_amount NUMBER(19,2) DEFAULT 0 NOT NULL);

-- Post-verify (optional):
--   SELECT pending_fine_amount FROM account WHERE ROWNUM <= 1;

-- Rollback (if needed):
--   ALTER TABLE account DROP COLUMN pending_fine_amount;
