-- Unified application schema fixes for Oracle (ACCOUNT_MS, TRANSACT_MS, OTP_MS)
-- Run as a privileged user (e.g., SYS as SYSDBA) OR run per-schema by removing the ALTER SESSION lines.
-- Idempotent: safe to re-run; it drops/recreates only what’s needed and checks for columns before adding.

set echo on
set serveroutput on
whenever sqlerror continue

prompt ===========================
prompt Switching to ACCOUNT_MS ...
prompt ===========================
BEGIN
  EXECUTE IMMEDIATE 'ALTER SESSION SET CURRENT_SCHEMA=ACCOUNT_MS';
  dbms_output.put_line('Switched CURRENT_SCHEMA to ACCOUNT_MS');
EXCEPTION
  WHEN OTHERS THEN
    dbms_output.put_line('Warning: could not set CURRENT_SCHEMA=ACCOUNT_MS - ' || SQLERRM);
END;
/

prompt ACCOUNT_MS: Fix ACCOUNT table CHECK constraints for account_type and balance...
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
    dbms_output.put_line('Warning: failed to drop explicitly named constraint ' || p_name || ' - ' || SQLERRM);
  END;

BEGIN
  -- Drop known/legacy named constraints if present
  drop_if_exists('CHK_ACCOUNT_TYPE_ALLOWED');
  drop_if_exists('CHK_BALANCE_BY_TYPE');

  -- Drop anonymous/conflicting CHECKs that reference ACCOUNT_TYPE or BALANCE >= 2000 (pattern-based)
  FOR rec IN (
    SELECT constraint_name, search_condition
      FROM user_constraints
     WHERE table_name = 'ACCOUNT'
       AND constraint_type = 'C'
       AND (UPPER(search_condition) LIKE '%ACCOUNT_TYPE%' OR UPPER(search_condition) LIKE '%BALANCE%>=%' )
  ) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNT DROP CONSTRAINT ' || rec.constraint_name;
      dbms_output.put_line('Dropped conflicting CHECK: ' || rec.constraint_name || ' :: ' || rec.search_condition);
    EXCEPTION
      WHEN OTHERS THEN
        dbms_output.put_line('Warning: could not drop ' || rec.constraint_name || ' - ' || SQLERRM);
    END;
  END LOOP;

  -- Re-add standardized constraints
  BEGIN
    EXECUTE IMMEDIATE '
      ALTER TABLE ACCOUNT ADD CONSTRAINT CHK_ACCOUNT_TYPE_ALLOWED
      CHECK (ACCOUNT_TYPE IN (''SAVINGS'', ''SALARY_CORPORATE'', ''CURRENT''))
    ';
    dbms_output.put_line('Created CHK_ACCOUNT_TYPE_ALLOWED.');
  EXCEPTION WHEN OTHERS THEN
    dbms_output.put_line('Notice: CHK_ACCOUNT_TYPE_ALLOWED already present or failed to create - ' || SQLERRM);
  END;

  BEGIN
    EXECUTE IMMEDIATE '
      ALTER TABLE ACCOUNT ADD CONSTRAINT CHK_BALANCE_BY_TYPE
      CHECK ( (ACCOUNT_TYPE <> ''SAVINGS'') OR (BALANCE >= 2000) )
    ';
    dbms_output.put_line('Created CHK_BALANCE_BY_TYPE.');
  EXCEPTION WHEN OTHERS THEN
    dbms_output.put_line('Notice: CHK_BALANCE_BY_TYPE already present or failed to create - ' || SQLERRM);
  END;
END;
/
show errors

prompt ACCOUNT_MS: Ensure ACCOUNT.STATUS allows ACTIVE, BLOCKED, CLOSED ...
DECLARE
  v_constraint_name VARCHAR2(200);
BEGIN
  BEGIN
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
    IF v_constraint_name IS NOT NULL THEN
      BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNT DROP CONSTRAINT ' || v_constraint_name;
        DBMS_OUTPUT.put_line('Dropped existing STATUS check constraint: ' || v_constraint_name);
      EXCEPTION WHEN OTHERS THEN
        DBMS_OUTPUT.put_line('Warning: failed to drop STATUS constraint ' || v_constraint_name || ' - ' || SQLERRM);
      END;
    END IF;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.put_line('No existing STATUS CHECK constraint found on ACCOUNT.STATUS.');
  END;

  BEGIN
    EXECUTE IMMEDIATE '
      ALTER TABLE ACCOUNT
      ADD CONSTRAINT CHK_ACCOUNT_STATUS_ALLOWED
      CHECK (STATUS IN (''ACTIVE'', ''BLOCKED'', ''CLOSED''))
    ';
    DBMS_OUTPUT.put_line('Created CHK_ACCOUNT_STATUS_ALLOWED.');
  EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.put_line('Notice: CHK_ACCOUNT_STATUS_ALLOWED already present or failed to create - ' || SQLERRM);
  END;
END;
/
show errors

prompt ACCOUNT_MS: Ensure ACCOUNT.PENDING_FINE_AMOUNT column exists (NUMBER(19,2) DEFAULT 0 NOT NULL) ...
DECLARE
  v_cnt NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO v_cnt
    FROM user_tab_columns
   WHERE table_name = 'ACCOUNT'
     AND column_name = 'PENDING_FINE_AMOUNT';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNT ADD (PENDING_FINE_AMOUNT NUMBER(19,2) DEFAULT 0 NOT NULL)';
    dbms_output.put_line('Added ACCOUNT.PENDING_FINE_AMOUNT');
  ELSE
    dbms_output.put_line('ACCOUNT.PENDING_FINE_AMOUNT already exists');
  END IF;
END;
/
show errors

prompt ==============================
prompt Switching to TRANSACT_MS ...
prompt ==============================
BEGIN
  EXECUTE IMMEDIATE 'ALTER SESSION SET CURRENT_SCHEMA=TRANSACT_MS';
  dbms_output.put_line('Switched CURRENT_SCHEMA to TRANSACT_MS');
EXCEPTION
  WHEN OTHERS THEN
    dbms_output.put_line('Warning: could not set CURRENT_SCHEMA=TRANSACT_MS - ' || SQLERRM);
END;
/

prompt TRANSACT_MS: Fix TRANSACTION.TRANSACTION_TYPE CHECK to include INTERNAL_DEBIT ...
DECLARE
  v_dropped NUMBER := 0;
BEGIN
  FOR rec IN (
    SELECT uc.constraint_name
      FROM user_constraints uc
      JOIN user_cons_columns ucc
        ON uc.constraint_name = ucc.constraint_name
     WHERE uc.table_name     = 'TRANSACTION'
       AND uc.constraint_type = 'C'
       AND ucc.column_name    = 'TRANSACTION_TYPE'
  ) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'ALTER TABLE "TRANSACTION" DROP CONSTRAINT ' || rec.constraint_name;
      v_dropped := v_dropped + 1;
      dbms_output.put_line('Dropped TRANSACTION_TYPE CHECK: ' || rec.constraint_name);
    EXCEPTION WHEN OTHERS THEN
      dbms_output.put_line('Skipping drop for ' || rec.constraint_name || ' due to: ' || SQLERRM);
    END;
  END LOOP;
  dbms_output.put_line('Total dropped TRANSACTION_TYPE CHECK constraints: ' || v_dropped);

  BEGIN
    EXECUTE IMMEDIATE '
      ALTER TABLE "TRANSACTION" ADD CONSTRAINT CK_TRANSACTION_TYPE
        CHECK (TRANSACTION_TYPE IN (''DEPOSIT'',''WITHDRAW'',''INTERNAL_DEBIT'',''TRANSFER'',''FINE''))
    ';
    dbms_output.put_line('Added CK_TRANSACTION_TYPE.');
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -2264 THEN -- ORA-02264: name already used by an existing constraint
        EXECUTE IMMEDIATE '
          ALTER TABLE "TRANSACTION" ADD CONSTRAINT CK_TRANSACTION_TYPE_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3') || '
            CHECK (TRANSACTION_TYPE IN (''DEPOSIT'',''WITHDRAW'',''INTERNAL_DEBIT'',''TRANSFER'',''FINE''))
        ';
        dbms_output.put_line('Created timestamped CK_TRANSACTION_TYPE due to name reuse.');
      ELSE
        dbms_output.put_line('Failed to add CK_TRANSACTION_TYPE: ' || SQLERRM);
      END IF;
  END;
END;
/
show errors

prompt ======================
prompt Switching to OTP_MS ...
prompt ======================
BEGIN
  EXECUTE IMMEDIATE 'ALTER SESSION SET CURRENT_SCHEMA=OTP_MS';
  dbms_output.put_line('Switched CURRENT_SCHEMA to OTP_MS');
EXCEPTION
  WHEN OTHERS THEN
    dbms_output.put_line('Warning: could not set CURRENT_SCHEMA=OTP_MS - ' || SQLERRM);
END;
/

prompt OTP_MS: Unify OTP_CODES.PURPOSE CHECK to include CARD_ISSUANCE and all required purposes ...
BEGIN
  -- Try dropping a known system-named constraint (ignore if missing)
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE OTP_CODES DROP CONSTRAINT SYS_C007813';
    dbms_output.put_line('Dropped SYS_C007813 on OTP_CODES.PURPOSE');
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE != -2443 THEN
        dbms_output.put_line('Could not drop SYS_C007813 (unexpected): ' || SQLERRM);
      ELSE
        dbms_output.put_line('SYS_C007813 not present (expected ok).');
      END IF;
  END;

  -- Drop any CHECK on PURPOSE by joining user_cons_columns (avoids LONG search_condition)
  DECLARE
    v_constraint_name VARCHAR2(128);
  BEGIN
    SELECT uc.constraint_name
      INTO v_constraint_name
      FROM user_constraints uc
      JOIN user_cons_columns ucc
        ON uc.constraint_name = ucc.constraint_name
     WHERE uc.table_name = 'OTP_CODES'
       AND uc.constraint_type = 'C'
       AND ucc.table_name = 'OTP_CODES'
       AND ucc.column_name = 'PURPOSE'
       AND ROWNUM = 1;
    EXECUTE IMMEDIATE 'ALTER TABLE OTP_CODES DROP CONSTRAINT ' || v_constraint_name;
    dbms_output.put_line('Dropped PURPOSE CHECK: ' || v_constraint_name);
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      dbms_output.put_line('No existing PURPOSE CHECK found on OTP_CODES.');
  END;

  -- Recreate explicit CHECK with all purposes
  BEGIN
    EXECUTE IMMEDIATE '
      ALTER TABLE OTP_CODES
        ADD CONSTRAINT CK_OTP_CODES_PURPOSE
        CHECK (PURPOSE IN (
          ''LOGIN'',
          ''WITHDRAWAL'',
          ''LOAN_SUBMISSION'',
          ''CARD_OPERATION'',
          ''CARD_ISSUANCE'',
          ''ACCOUNT_OPERATION'',
          ''CONTACT_VERIFICATION''
        ))
    ';
    dbms_output.put_line('Created CK_OTP_CODES_PURPOSE with all required values.');
  EXCEPTION WHEN OTHERS THEN
    dbms_output.put_line('Notice: failed to create CK_OTP_CODES_PURPOSE - ' || SQLERRM);
  END;
END;
/
show errors

prompt Unified schema fixes completed.
