-- Fix Account STATUS check constraint to allow BLOCKED in addition to ACTIVE and CLOSED
-- This script safely drops the existing STATUS check constraint (even if system-named)
-- and recreates a named constraint CHK_ACCOUNT_STATUS_ALLOWED.

-- Notes:
-- - Run this in the AccountMicroservice Oracle schema (e.g., ACCOUNT_MS).
-- - If you use a different schema, ensure you connect to that schema first.
-- - This script is idempotent: it will drop any existing STATUS check constraint and recreate the correct one.

SET SERVEROUTPUT ON;

DECLARE
  v_constraint_name VARCHAR2(200);
BEGIN
  -- Attempt to find an existing check constraint on ACCOUNT.STATUS
  SELECT uc.constraint_name
    INTO v_constraint_name
    FROM user_constraints uc
    JOIN user_cons_columns ucc
      ON uc.constraint_name = ucc.constraint_name
   WHERE uc.table_name = 'ACCOUNT'
     AND uc.constraint_type = 'C'
     AND ucc.table_name = 'ACCOUNT'
     AND ucc.column_name = 'STATUS'
     AND ROWNUM = 1;

  -- If found, drop it
  IF v_constraint_name IS NOT NULL THEN
    BEGIN
      EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNT DROP CONSTRAINT ' || v_constraint_name;
      DBMS_OUTPUT.put_line('Dropped existing STATUS check constraint: ' || v_constraint_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.put_line('Warning: failed to drop existing constraint ' || v_constraint_name || ' - ' || SQLERRM);
    END;
  END IF;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.put_line('No existing STATUS check constraint found on ACCOUNT.STATUS. Proceeding to create.');
  WHEN OTHERS THEN
    DBMS_OUTPUT.put_line('Warning: error while locating STATUS constraint - ' || SQLERRM);
END;
/

-- Create the desired check constraint explicitly allowing ACTIVE, BLOCKED, CLOSED
BEGIN
  EXECUTE IMMEDIATE '
    ALTER TABLE ACCOUNT
    ADD CONSTRAINT CHK_ACCOUNT_STATUS_ALLOWED
    CHECK (STATUS IN (''ACTIVE'', ''BLOCKED'', ''CLOSED''))
  ';
  DBMS_OUTPUT.put_line('Created CHK_ACCOUNT_STATUS_ALLOWED allowing ACTIVE, BLOCKED, CLOSED.');
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.put_line('Error creating CHK_ACCOUNT_STATUS_ALLOWED - ' || SQLERRM);
    RAISE;
END;
/

COMMIT;
