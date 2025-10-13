# Banking Microservices – Local Setup Guide (Windows/PowerShell)

This document describes the complete local setup to run the Banking microservices, API Gateway, and Angular frontends, including Keycloak, Kafka, Eureka, Oracle DB, and later enabling the Notification Service.

Tested on:
- OS: Windows 11
- Shell: PowerShell 7 (pwsh)
- Java 17, Node 20.x, Maven Wrapper per module

Repository root: c:/Users/faihussa/Downloads/projectbanking


## Prerequisites

- Java 17 JDK in PATH
- Node.js 20.x and npm
- Git
- Oracle DB (listener on localhost:1521 with service ORCL)
- Kafka 2.8.0 + ZooKeeper
- Keycloak 26.3.5 reachable at http://localhost:8080
- Free ports: 8761 (Eureka), 8888 (Config Server), 9010/9011 (Gateway), 4200 (Frontend), 4300 (Admin Frontend), and the service ports below


## Ports Overview

- Keycloak: 8080
- Spring Cloud Config Server: 8888
- Eureka (discovery): 8761
- API Gateway: 9010 (primary), 9011 (second instance optional)
- Microservices:
  - USER-SERVICE: 8000 (schema: AUTH_MS/auth123)
  - TRANSACTION-SERVICE: 8001 (schema: TRANSACT_MS/transact123)
  - ACCOUNT-SERVICE: 8003 (schema: ACCOUNT_MS/account123)
  - LOAN-SERVICE: 8004 (schema: LOAN_MS/loan123)
  - CREDIT-CARD-SERVICE: 8005 (schema: CARD_MS/card123)
  - NOTIFICATION-SERVICE: (enable later)
- Angular apps:
  - banking-frontend: 4200
  - banking-admin-dashboard: 4300


## Repository Layout (relevant)

- bank-config-repo/ … central config files (Spring Cloud Config)
- Banking-Project-Microservice/
  - api-gateway/api-gateway/
  - config-server/config-server/
  - discovery-server/discovery-server/ (not used; use local-eureka instead)
  - local-eureka/ (standalone Eureka server, recommended locally)
  - UserMicroservice/
  - AccountMicroservice/
  - TransactionService/
  - loan-service/loan-service/
  - CreditCardService/
  - NotificationService/ (enable later)
  - Angular Frontend/
    - banking-frontend/
    - banking-admin-dashboard/
- db/setup_orcl_schemas_direct.sql
- scripts/keycloak/
  - setup-clients-rest.ps1 (create Keycloak public clients for Angular apps)
  - create-test-user-and-test.ps1 (create a user and test gateway protected routes)


## 1) Start Spring Cloud Config Server

From repo root:
```
cd "Banking-Project-Microservice/config-server/config-server"
.\mvnw.cmd -DskipTests spring-boot:run
```
Check: http://localhost:8888


## 2) Start Eureka (discovery)

Use the standalone Eureka provided (local-eureka):
```
cd "local-eureka"
mvn spring-boot:run
```
Check: http://localhost:8761


## 3) Prepare Oracle Database Schemas

Use SQL*Plus or your Oracle tool to run:
```
db\setup_orcl_schemas_direct.sql
```
This script creates/grants schemas:
- AUTH_MS/auth123
- ACCOUNT_MS/account123
- TRANSACT_MS/transact123
- LOAN_MS/loan123
- CARD_MS/card123
- NOTIFY_MS/notify123
- OTP_MS/otp123
- SELF_SERVICE_MS/selfservice123

Ensure microservices point to:
- URL: jdbc:oracle:thin:@//localhost:1521/ORCL
- Username/Password per-service above.


## 4) Start ZooKeeper and Kafka, and create topics

Start ZooKeeper (example):
```
"c:\kafka\bin\windows\zookeeper-server-start.bat" "c:\kafka\config\zookeeper.properties"
```
Start Kafka:
```
"c:\kafka\bin\windows\kafka-server-start.bat" "c:\kafka\config\server.properties"
```
Create topics:
```
"c:\kafka\bin\windows\kafka-topics.bat" --bootstrap-server localhost:9092 --create --topic transaction-events --partitions 1 --replication-factor 1
"c:\kafka\bin\windows\kafka-topics.bat" --bootstrap-server localhost:9092 --create --topic kyc-status-events --partitions 1 --replication-factor 1
"c:\kafka\bin\windows\kafka-topics.bat" --bootstrap-server localhost:9092 --create --topic loan-status-events --partitions 1 --replication-factor 1
```


## 5) Keycloak setup (realm, roles, clients)

Keycloak Admin Console:
- URL: http://localhost:8080
- Admin credentials: admin / admin

Realm:
- Create realm: bank-realm (if not already created)

Realm roles:
- ADMIN
- CUSTOMER

Clients:
- Public OIDC clients for Angular:
  - bank-frontend
  - bank-admin-frontend
- Confidential service accounts (already noted):
  - user-service-admin-client (grant: realm-management/manage-users)
  - notification-service-client

Automated creation of the Angular public clients (recommended):
```
pwsh -NoProfile -File "scripts/keycloak/setup-clients-rest.ps1" -KeycloakUrl http://localhost:8080 -Realm bank-realm -AdminUser admin -AdminPassword admin
```
This creates/updates:
- bank-frontend: redirect http://localhost:4200/*, web origins http://localhost:4200
- bank-admin-frontend: redirect http://localhost:4300/*, web origins http://localhost:4300
- PKCE S256 enabled, Standard Flow enabled

Known service client secrets (for reference; not used by human logins):
- user-service-admin-client (confidential): SvHZfh0PCQrV22qhm3rHSvyhxdtfRgev
- notification-service-client (confidential): UQCtpCaZ02y9DQK8sMUcm03G9A6p88vD

JWKS endpoint (gateway uses it for JWT validation):
- http://localhost:8080/realms/bank-realm/protocol/openid-connect/certs


## 6) Start microservices

General pattern (from repo root):
```
cd "Banking-Project-Microservice/<ServiceDir>"
.\mvnw.cmd -DskipTests spring-boot:run
```

Services and directories:
- UserMicroservice/
<!-- cd Banking-Project-Microservice/UserMicroservice
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run -->
- AccountMicroservice/
<!-- cd Banking-Project-Microservice/AccountMicroservice
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run -->
- TransactionService/
<!-- cd Banking-Project-Microservice/TransactionService
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run -->
- loan-service/loan-service/
<!-- cd Banking-Project-Microservice/loan-service/loan-service
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run -->
- CreditCardService/
<!-- cd Banking-Project-Microservice/CreditCardService
mvn spring-boot:run -->
- NotificationService/
<!-- cd Banking-Project-Microservice/NotificationService
mvn spring-boot:run -->
- OTP-Service/
<!-- cd Banking-Project-Microservice/otp-service
mvn spring-boot:run -->
- Self Service/
<!-- cd Banking-Project-Microservice/SelfService
mvn spring-boot:run -->

Each service should register with Eureka at http://localhost:8761 and use the Oracle schema noted above. Check application.yaml for:
- eureka.client.serviceUrl.defaultZone: http://localhost:8761/eureka
- spring.datasource.url: jdbc:oracle:thin:@//localhost:1521/ORCL
- spring.datasource.username/password per schema
- Keycloak JWT: jwk-set-uri: http://localhost:8080/realms/bank-realm/protocol/openid-connect/certs

Confirm health per service (replace port accordingly), e.g.:
```
curl http://localhost:8000/actuator/health
```


## 7) API Gateway

Build:
```
cd "Banking-Project-Microservice/api-gateway/api-gateway" && .\mvnw.cmd spring-boot:run
```

Run JAR (PowerShell 7; quote -D flags):
```
java "-Dserver.port=9011" "-Dspring.main.web-application-type=reactive" "-Dlogging.level.root=INFO" "-Dlogging.level.org.springframework=INFO" -jar "target\api-gateway-0.0.1-SNAPSHOT.jar"
```
By default server.port in application.yaml is 9010; the command above starts a second instance on 9011. You may run only one:
```
java -jar "target\api-gateway-0.0.1-SNAPSHOT.jar"
```

Check:
- Health: http://localhost:9010/actuator/health
- Eureka registration: http://localhost:8761

Note on actuator path through gateway:
- Current route rewrites preserve service base paths (e.g., /accounts/**). Thus calling /accounts/actuator/health will forward to /accounts/actuator/health on the downstream, which does not exist (actuator is /actuator/**).
- If you need proxied actuator access via the gateway, add dedicated routes that rewrite /{svc}/actuator/** to /actuator/**.


## 8) Angular frontends

banking-frontend (Customer UI):
```
cd "Banking-Project-Microservice/Angular Frontend/banking-frontend"
npm ci
npm start
# http://localhost:4200
```
OIDC client: bank-frontend (public, Standard Flow)

banking-admin-dashboard (Admin UI):
```
cd "Banking-Project-Microservice/Angular Frontend/banking-admin-dashboard"
ng serve --port 4300
# http://localhost:4300
```
OIDC client: bank-admin-frontend (public)

If login fails with “Client not found”:
- Ensure you ran scripts/keycloak/setup-clients-rest.ps1
- Ensure realm is bank-realm and Keycloak is at 8080


## 9) Create a test user and test gateway protected routes

Create or update a test user (customer1) and test calls through the gateway using a token:
```
pwsh -NoProfile -File "scripts/keycloak/create-test-user-and-test.ps1" -KeycloakUrl http://localhost:8080 -Realm bank-realm -AdminUser admin -AdminPassword admin -Username customer1 -Password Customer@123 -Email customer1@example.com -GatewayBase http://localhost:9010
```
This will:
- Ensure user exists with password set and profile completed
- Obtain a token via Direct Access Grants on bank-frontend
- Attempt calls to:
  - /accounts/actuator/health
  - /transactions/actuator/health
  - /loans/actuator/health
  - /cards/actuator/health
Note the actuator rewrite caveat described earlier.


## 10) Enable Notification Service (later activation)

Requirements:
- Gmail sender address
- Gmail App Password (NOT your normal password). Follow Google’s 2FA + App Password docs.

Configuration (env vars or application.yaml overrides):
- SPRING_MAIL_HOST=smtp.gmail.com
- SPRING_MAIL_PORT=587
- SPRING_MAIL_USERNAME=<your-gmail-address>
- SPRING_MAIL_PASSWORD=<your-app-password>
- SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
- SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
- SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

Start NotificationService:
```
cd "Banking-Project-Microservice/NotificationService"
.\mvnw.cmd -DskipTests spring-boot:run
```
Ensure Kafka is running and relevant topics exist (e.g., notification triggering topic per service integration).
Verify logs for email send on consumed events. If services publish to specific topics (e.g., transaction-events), confirm NotificationService subscribes to the configured topics.


## 11) Troubleshooting

- PowerShell and JVM -D flags:
  - Always quote -D flags in PowerShell 7:
    ```
    java "-Dserver.port=9011" -jar app.jar
    ```
  - Without quotes you may see: “Could not find or load main class .port=9011”.

- 401 Unauthorized from gateway on service paths:
  - Expected when accessing protected routes without a Bearer token.
  - Use the Angular app for interactive auth, or obtain a token via OAuth and include Authorization: Bearer <token>.

- 500 “No static resource {path}” on actuator calls:
  - Caused by route rewrites preserving the service prefix. Add dedicated routes that rewrite /{svc}/actuator/** to /actuator/**, or call the services’ actuator endpoints directly (http://localhost:{port}/actuator/**).

- ORA-12514 / Oracle connectivity:
  - Use correct service name: jdbc:oracle:thin:@//localhost:1521/ORCL
  - Verify listener is up, schemas exist, credentials match.

- Eureka server conflict:
  - If the project’s discovery-server fails locally, use the provided local-eureka module as shown above.

- API Gateway build warnings:
  - api-gateway/pom.xml contains duplicate dependencies. Cleanup is recommended to silence Maven warnings, but current build is functional.
  - Dependency spring-cloud-starter-gateway-server-webflux is nonstandard; canonical starter is spring-cloud-starter-gateway. Current setup works with server-webflux starter.


## 12) Credentials and Secrets Summary

- Keycloak Admin Console:
  - URL: http://localhost:8080
  - Username: admin
  - Password: admin

- Keycloak realm: bank-realm

- Angular OIDC clients:
  - bank-frontend (public) – redirect http://localhost:4200/*
  - bank-admin-frontend (public) – redirect http://localhost:4300/*

- Service clients:
  - user-service-admin-client (confidential, service account)
    - Secret: SvHZfh0PCQrV22qhm3rHSvyhxdtfRgev
    - Granted: realm-management/manage-users
  - notification-service-client (confidential, service account)
    - Secret: UQCtpCaZ02y9DQK8sMUcm03G9A6p88vD

- DB Schemas (Oracle ORCL):
  - AUTH_MS/auth123
  - ACCOUNT_MS/account123
  - TRANSACT_MS/transact123
  - LOAN_MS/loan123
  - CARD_MS/card123
  - NOTIFY_MS/<choose-password> (if required)

- Kafka Topics:
  - transaction-events
  - kyc-status-events
  - loan-status-events


## 13) Useful Commands

Health checks:
```
curl http://localhost:9010/actuator/health
curl http://localhost:9011/actuator/health
```

Eureka apps:
```
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
```

Keycloak JWKS:
```
curl http://localhost:8080/realms/bank-realm/protocol/openid-connect/certs
```

Start admin dashboard on port 4300:
```
cd "Banking-Project-Microservice/Angular Frontend/banking-admin-dashboard"
npm ci
npm start -- --port 4300
# http://localhost:4300
```

Create a real admin user for the Admin UI (via UI):
- Keycloak Admin Console → bank-realm → Users → Add user
  - username: admin1, email: admin1@example.com, Enabled: ON, Email verified: ON
- Credentials → set password: Admin@123 (Temporary: OFF)
- Roles → Assign role: ADMIN


## New Endpoints and Frontend Flows (Debit Card Withdraw + PAN Reveal)

API Gateway
- Proxies already exist:
  - /transactions/** -> TRANSACTION-SERVICE
  - /cards/** -> CREDIT-CARD-SERVICE
- CORS: http://localhost:4200 and http://localhost:4300 allowed.
- Security: Gateway is open (permitAll); microservices enforce JWT.

CreditCardService (JWT required)
- POST /cards/{id}/reveal-pan
  - Purpose: Reveal full PAN for user’s own DEBIT or CREDIT card, OTP-gated.
  - Request body:
    {
      "userId": "<keycloak-sub>",
      "otpCode": "<otp-6-digit>"
    }
  - OTP purpose: CARD_OPERATION
  - OTP contextId: accountId linked to the card (handled server-side)
  - Validation: ownership, ACTIVE, not expired, OTP verify
  - Audit: logs attempts; basic rate limit (5 per 10 minutes per userId+cardId)
  - Response: { cardId, fullPan, message }
  - Error contract: domain/business errors (e.g., card expired, wrong user) return 400 with { "message": "..." }; generic 500s return { "message": "Internal error" }.

- POST /cards/{id}/regenerate-cvv
  - Purpose: Securely regenerate CVV for user’s own DEBIT card. Original CVV cannot be revealed; new CVV is generated and returned once.
  - Request body:
    {
      "userId": "<keycloak-sub>",
      "otpCode": "<otp-6-digit>"
    }
  - OTP purpose: CARD_OPERATION
  - OTP contextId: accountId linked to the card (handled server-side)
  - Validation: ownership, DEBIT-only, ACTIVE, not expired, OTP verify
  - Rate limit: max 3 attempts per 30 minutes per userId+cardId
  - Persistence: only SHA-256 hash of the new CVV is stored (no plaintext)
  - Notification: email sent to user (no plaintext CVV in email)
  - Response: { cardId, cvv, message } — cvv is shown once in the API response; UI must not persist it beyond the one-time display.
  - Error contract: business errors -> 400 { "message": "..." }; generic -> 500 { "message": "Internal error" }.

TransactionService (JWT required)
- POST /transactions/debit-card/withdraw
  - Purpose: Withdraw using a DEBIT card (not credit). Validates CVV length by brand/account type, card status/expiry, ownership, and OTP.
  - Request body:
    {
      "cardNumber": "string",
      "cvv": "string",
      "amount": number,
      "otpCode": "string"
    }
  - OTP purpose: WITHDRAWAL
  - Response metadata: metadataJson contains method, brand, panMasked for UI/notifications

Angular Customer App (banking-frontend)
- Withdraw page:
  - Mode selector: Account or Debit Card
  - Debit Card requires cardNumber, CVV, OTP; calls POST /transactions/debit-card/withdraw
  - UX polish added:
    - Button loaders (Processing...), disable during submit
    - OTP Generate button with cooldown (Resend in Ns)
    - Success message shows transaction id and masked card (when provided)
- Card Management:
  - Reveal PAN flow (DEBIT and CREDIT):
    - “Get OTP” (purpose CARD_OPERATION) + input + Reveal
    - Per-card loading and cooldown for OTP requests
    - Shows full PAN for session on success
  - CVV is never revealed; masked length reflects brand/account type.

Notes
- Database migrations unified (Oracle):
  - Run db/migrations/20251011_unified_app_schema_fixes.sql (as SYS or per-schema by removing ALTER SESSION lines). It applies:
    - ACCOUNT_MS: standardized CHECKs (account_type whitelist, balance rule for SAVINGS only), STATUS allowed values (ACTIVE, BLOCKED, CLOSED), and adds PENDING_FINE_AMOUNT if missing
    - TRANSACT_MS: TRANSACTION_TYPE CHECK includes INTERNAL_DEBIT
    - OTP_MS: CK_OTP_CODES_PURPOSE includes CARD_ISSUANCE and all required purposes
- Security:
  - CreditCardService and TransactionService require authenticated requests; roles mapped from Keycloak JWT.
- Observability/Protection:
  - Basic in-memory rate-limiting and audit logs for PAN reveal attempts.

Smoke Test (after services start and Angular running)
1) Login via Angular at http://localhost:4200
2) Withdraw:
   - Account mode: generate OTP (WITHDRAWAL), withdraw small amount
   - Debit Card mode: enter cardNumber, CVV, amount, OTP; verify success message and metadata
3) Card Management:
   - Click Get OTP (CARD_OPERATION), enter OTP, click Reveal; full PAN should display for that session
4) Check Transaction History:
   - “Method” column shows Debit Card/Account, brand, masked PAN when available.

## End-to-End Order of Startup

1) Oracle DB listener and schemas ready
2) ZooKeeper and Kafka (with topics created)
3) Keycloak on http://localhost:8080 (bank-realm present)
4) Spring Cloud Config Server on 8888
5) Eureka (local-eureka) on 8761
6) Microservices (User, Account, Loan, Transaction, Credit Card)
7) API Gateway on 9010 (or 9011)
8) Angular apps (frontend 4200, admin 4300)
9) (Optional) NotificationService after configuring mail and Kafka
