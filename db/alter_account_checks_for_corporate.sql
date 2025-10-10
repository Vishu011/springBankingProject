-- Purpose:
--   Fix ACCOUNT table CHECK constraints to support SALARY_CORPORATE accounts and correct the min-balance rule.
--   - Ensure account_type allows 'SALARY_CORPORATE' (retain 'SAVINGS' and legacy 'CURRENT' if present).
--   - Enforce balance >= 2000 only for SAVINGS; no min-balance for SALARY_CORPORATE.
--
-- How to run (as ACCOUNT_MS or table owner):
--   sqlplus ACCOUNT_MS/<password>@<host>:<port>/<service> @db/alter_account_checks_for_corporate.sql
--
-- Notes:
--   - This script attempts to drop only conflicting CHECK constraints and re-adds properly named ones.
--   - It is safe if run multiple times.

set echo on
set serveroutput on
whenever sqlerror continue

prompt Showing current CHECK constraints on ACCOUNT before changes...
SELECT constraint_name, search_condition
FROM   user_constraints
WHERE  table_name = 'ACCOUNT'
AND    constraint_type = 'C'
ORDER  BY constraint_name;

DECLARE
  PROCEDURE drop_if_exists(p_name IN VARCHAR2) IS
    v_cnt NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_cnt
    FROM user_constraints
    WHERE table_name = 'ACCOUNT'
      AND constraint_name = UPPER(p_name)
      AND constraint_type = 'C';
    IF v_cnt > 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNT DROP CONSTRAINT ' || p_name;
      dbms_output.put_line('Dropped constraint: ' || p_name);
    END IF;
  EXCEPTION WHEN OTHERS THEN
    dbms_output.put_line('Warning: failed to drop constraint ' || p_name || ' - ' || SQLERRM);
  END;

  -- Drop any anonymous SYS_* CHECK constraints that likely enforce old rules on account_type or balance >= 2000
  PROCEDURE drop_conflicting_checks IS
  BEGIN
    FOR rec IN (
      SELECT constraint_name, search_condition
      FROM user_constraints
      WHERE table_name = 'ACCOUNT'
        AND constraint_type = 'C'
        AND (
             UPPER(search_condition) LIKE '%ACCOUNT_TYPE%'
          OR UPPER(search_condition) LIKE '%BALANCE%>=%2000%'
          OR UPPER(search_condition) LIKE '%BALANCE%>=%201999%' -- safeguard for spacing variants (no-op)
          OR UPPER(search_condition) LIKE '%BALANCE%>=%2000%'
          OR UPPER(search_condition) LIKE '%BALANCE >= 2000%'
        )
    ) LOOP
      BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNT DROP CONSTRAINT ' || rec.constraint_name;
        dbms_output.put_line('Dropped conflicting CHECK: ' || rec.constraint_name || ' :: ' || rec.search_condition);
      EXCEPTION WHEN OTHERS THEN
        dbms_output.put_line('Warning: could not drop ' || rec.constraint_name || ' - ' || SQLERRM);
      END;
    END LOOP;
  END;
BEGIN
  -- Explicitly try the known failing SYS constraint (if it exists)
  drop_if_exists('SYS_C007745');
  -- Drop any other conflicting checks that match old patterns
  drop_conflicting_checks;
END;
/
show errors

prompt Re-adding standardized CHECK constraints...

-- Allowable account_type values. Keep legacy CURRENT if rows exist; safe to allow even if app no longer uses it.
ALTER TABLE ACCOUNT ADD CONSTRAINT CHK_ACCOUNT_TYPE_ALLOWED
CHECK (ACCOUNT_TYPE IN ('SAVINGS', 'SALARY_CORPORATE', 'CURRENT'));

-- Min-balance only for SAVINGS; corporate has no minimum.
ALTER TABLE ACCOUNT ADD CONSTRAINT CHK_BALANCE_BY_TYPE
CHECK ( (ACCOUNT_TYPE <> 'SAVINGS') OR (BALANCE >= 2000) );

prompt Showing CHECK constraints on ACCOUNT after changes...
SELECT constraint_name, search_condition
FROM   user_constraints
WHERE  table_name = 'ACCOUNT'
AND    constraint_type = 'C'
ORDER  BY constraint_name;

prompt Done. Retry creating a SALARY_CORPORATE account now.
