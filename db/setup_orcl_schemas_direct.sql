-- Run this as SYS on ORCL to create schemas without using stored procedures.
-- Example:
-- sqlplus sys/<SYS_PASSWORD>@localhost:1521/ORCL as sysdba @db/setup_orcl_schemas_direct.sql

set echo on
set serveroutput on
whenever sqlerror continue

prompt Dropping users if they already exist (errors ignored)...
drop user AUTH_MS cascade;
drop user ACCOUNT_MS cascade;
drop user TRANSACT_MS cascade;
drop user NOTIFY_MS cascade;
drop user LOAN_MS cascade;
drop user CARD_MS cascade;

prompt Creating users...
create user AUTH_MS identified by auth123;
create user ACCOUNT_MS identified by account123;
create user TRANSACT_MS identified by transact123;
create user NOTIFY_MS identified by notify123;
create user LOAN_MS identified by loan123;
create user CARD_MS identified by card123;

prompt Unlocking accounts...
alter user AUTH_MS account unlock;
alter user ACCOUNT_MS account unlock;
alter user TRANSACT_MS account unlock;
alter user NOTIFY_MS account unlock;
alter user LOAN_MS account unlock;
alter user CARD_MS account unlock;

prompt Granting privileges (sufficient for Spring JPA/Hibernate in dev)...
grant create session to AUTH_MS;
grant create table to AUTH_MS;
grant create sequence to AUTH_MS;
grant create view to AUTH_MS;
grant create procedure to AUTH_MS;
grant create trigger to AUTH_MS;
grant connect to AUTH_MS;
grant resource to AUTH_MS;

grant create session to ACCOUNT_MS;
grant create table to ACCOUNT_MS;
grant create sequence to ACCOUNT_MS;
grant create view to ACCOUNT_MS;
grant create procedure to ACCOUNT_MS;
grant create trigger to ACCOUNT_MS;
grant connect to ACCOUNT_MS;
grant resource to ACCOUNT_MS;

grant create session to TRANSACT_MS;
grant create table to TRANSACT_MS;
grant create sequence to TRANSACT_MS;
grant create view to TRANSACT_MS;
grant create procedure to TRANSACT_MS;
grant create trigger to TRANSACT_MS;
grant connect to TRANSACT_MS;
grant resource to TRANSACT_MS;

grant create session to NOTIFY_MS;
grant create table to NOTIFY_MS;
grant create sequence to NOTIFY_MS;
grant create view to NOTIFY_MS;
grant create procedure to NOTIFY_MS;
grant create trigger to NOTIFY_MS;
grant connect to NOTIFY_MS;
grant resource to NOTIFY_MS;

grant create session to LOAN_MS;
grant create table to LOAN_MS;
grant create sequence to LOAN_MS;
grant create view to LOAN_MS;
grant create procedure to LOAN_MS;
grant create trigger to LOAN_MS;
grant connect to LOAN_MS;
grant resource to LOAN_MS;

grant create session to CARD_MS;
grant create table to CARD_MS;
grant create sequence to CARD_MS;
grant create view to CARD_MS;
grant create procedure to CARD_MS;
grant create trigger to CARD_MS;
grant connect to CARD_MS;
grant resource to CARD_MS;

prompt Granting unlimited quota on USERS tablespace (adjust if you use a different default TS)...
alter user AUTH_MS quota unlimited on USERS;
alter user ACCOUNT_MS quota unlimited on USERS;
alter user TRANSACT_MS quota unlimited on USERS;
alter user NOTIFY_MS quota unlimited on USERS;
alter user LOAN_MS quota unlimited on USERS;
alter user CARD_MS quota unlimited on USERS;

prompt Done. Verify with: select username, account_status from dba_users where username in ('AUTH_MS','ACCOUNT_MS','TRANSACT_MS','NOTIFY_MS','LOAN_MS','CARD_MS');
