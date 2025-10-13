-- Adds middle_name column to user_table if it does not already exist (Oracle-compatible)
-- Safe to run multiple times.

DECLARE
  v_count NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM USER_TAB_COLS
  WHERE TABLE_NAME = 'USER_TABLE' AND COLUMN_NAME = 'MIDDLE_NAME';

  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE user_table ADD (middle_name VARCHAR2(100))';
  END IF;
END;
/

-- Optional: verify structure
-- SELECT column_name, data_type, data_length FROM USER_TAB_COLS WHERE TABLE_NAME = 'USER_TABLE' ORDER BY column_id;
