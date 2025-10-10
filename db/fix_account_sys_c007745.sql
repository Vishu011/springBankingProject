-- Purpose:
--   Resolve ORA-02290 on ACCOUNT insert by removing the offending check constraint
--   (ACCOUNT_MS.SYS_C007745) and ensuring correct constraints for:
--     - account_type whitelist includes 'SALARY_CORPORATE'
--     - min-balance (>= 2000) applies only to SAVINGS
--
-- How to run (as ACCOUNT_MS or the table owner):
--   sqlplus ACCOUNT_MS/<password>@<host>:<port>/<service> @db/fix_account_sys_c007745.sql
--
-- Notes:
--   - DDL auto-commits in Oracle.
--   - Script is idempotent: only adds constraints if missing; drops SYS_C007745 if present.

set echo on
set serveroutput on
whenever sqlerror continue

prompt Current CHECK constraints on ACCOUNT (before)...
SELECT constraint_name, search_condition
FROM   user_constraints
WHERE  table_name = 'ACCOUNT'
AND    constraint_type = 'C'
ORDER  BY constraint_name;

DECLARE
  FUNCTION has_constraint(p_name VARCHAR2) RETURN NUMBER IS
    v_cnt NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_cnt
    FROM user_constraints
    WHERE table_name = 'ACCOUNT'
      AND constraint_name = UPPER(p_name)
      AND constraint_type = 'C';
    RETURN v_cnt;
  END;

  PROCEDURE drop_constraint_safe(p_name VARCHAR2) IS
  BEGIN
    IF has_constraint(p_name) > 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNT DROP CONSTRAINT ' || p_name;
      dbms_output.put_line('Dropped constraint: ' || p_name);
    ELSE
      dbms_output.put_line('Constraint not present, skip drop: ' || p_name);
    END IF;
  EXCEPTION WHEN OTHERS THEN
    dbms_output.put_line('Warning: failed to drop ' || p_name || ' - ' || SQLERRM);
  END;

  PROCEDURE add_account_type_check IS
  BEGIN
    IF has_constraint('CHK_ACCOUNT_TYPE_ALLOWED') = 0 THEN
      EXECUTE IMMEDIATE q'[
        ALTER TABLE ACCOUNT ADD CONSTRAINT CHK_ACCOUNT_TYPE_ALLOWED
        CHECK (ACCOUNT_TYPE IN ('SAVINGS','SALARY_CORPORATE','CURRENT'))
      ]';
      dbms_output.put_line('Added CHK_ACCOUNT_TYPE_ALLOWED');
    ELSE
      dbms_output.put_line('CHK_ACCOUNT_TYPE_ALLOWED already exists');
    END IF;
  EXCEPTION WHEN OTHERS THEN
    dbms_output.put_line('Warning: failed to add CHK_ACCOUNT_TYPE_ALLOWED - ' || SQLERRM);
  END;

  PROCEDURE add_balance_by_type_check IS
  BEGIN
    IF has_constraint('CHK_BALANCE_BY_TYPE') = 0 THEN
      EXECUTE IMMEDIATE q'[
        ALTER TABLE ACCOUNT ADD CONSTRAINT CHK_BALANCE_BY_TYPE
        CHECK ( (ACCOUNT_TYPE <> 'SAVINGS') OR (BALANCE >= 2000) )
      ]';
      dbms_output.put_line('Added CHK_BALANCE_BY_TYPE');
    ELSE
      dbms_output.put_line('CHK_BALANCE_BY_TYPE already exists');
    END IF;
  EXCEPTION WHEN OTHERS THEN
    dbms_output.put_line('Warning: failed to add CHK_BALANCE_BY_TYPE - ' || SQLERRM);
  END;

BEGIN
  -- Explicitly drop the failing SYS check constraint if present.
  drop_constraint_safe('SYS_C007745');

  -- Ensure correct, named constraints exist (safe if already present).
  add_account_type_check;
  add_balance_by_type_check;
END;
/
show errors

prompt Current CHECK constraints on ACCOUNT (after)...
SELECT constraint_name, search_condition
FROM   user_constraints
WHERE  table_name = 'ACCOUNT'
AND    constraint_type = 'C'
ORDER  BY constraint_name;

prompt Done. Retry creating a SALARY_CORPORATE account now.
