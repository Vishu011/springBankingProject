-- Oracle setup script for SelfService microservice schema
-- Run this script as SYS or SYSTEM (or another DBA user) in SQL*Plus/SQLcl/SQL Developer
-- Adjust the password below before executing.

-- 1) Create application user/schema (schema name is the same as user in Oracle)
CREATE USER SELF_SERVICE_MS IDENTIFIED BY "selfservice123"
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP
  ACCOUNT UNLOCK;

-- 2) Grant essential privileges (least-privilege set for JPA/Hibernate to manage tables)
GRANT CREATE SESSION TO SELF_SERVICE_MS;
GRANT CREATE TABLE TO SELF_SERVICE_MS;
GRANT CREATE SEQUENCE TO SELF_SERVICE_MS;
GRANT CREATE VIEW TO SELF_SERVICE_MS;
GRANT CREATE PROCEDURE TO SELF_SERVICE_MS;
GRANT CREATE TRIGGER TO SELF_SERVICE_MS;
GRANT CREATE TYPE TO SELF_SERVICE_MS;

-- 3) Allow the user to allocate space in the default USERS tablespace
ALTER USER SELF_SERVICE_MS QUOTA UNLIMITED ON USERS;

-- Optional (legacy roles, not recommended for production hardening; uncomment if needed during dev)
-- GRANT CONNECT, RESOURCE TO SELF_SERVICE_MS;

-- Verification (optional):
-- SELECT username, account_status, default_tablespace, temporary_tablespace FROM dba_users WHERE username = 'SELF_SERVICE_MS';
-- SELECT * FROM dba_ts_quotas WHERE username = 'SELF_SERVICE_MS';

-- Connection info for the app (example):
-- spring.datasource.url=jdbc:oracle:thin:@localhost:1521:orcl
-- spring.datasource.username=SELF_SERVICE_MS
-- spring.datasource.password=ChangeThis_StrongPwd1!
-- spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
-- spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
