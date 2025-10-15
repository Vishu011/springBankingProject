# OMNI Bank — Deep-Dive Architecture and Feature Documentation

Goal of this document:
- Give a detailed, practical explanation of what each service does
- Show every feature with its endpoint(s), required inputs, outputs, validations, and security rules
- Explain the tools/libraries used to implement each capability (e.g., Feign, Kafka, PDFBox, OCR, LLM, etc.)
- Describe end-to-end workflows across services, including OTP, notifications, and admin automation with the AI Orchestrator
- Keep language simple and descriptive for fast onboarding

Important note on secrets: Move sensitive values (e.g., OAuth2 client secrets) out of the repo and into environment variables or a secrets manager before production.

--------------------------------------------------------------------------------

# 1) Big Picture

- Entry point: API Gateway on port 9010 routes Angular frontends to microservices.
- Service Discovery: Eureka. The gateway and services use lb://service-id naming.
- Authentication/Authorization: Keycloak (OIDC). Services use Spring Security method-level rules (@PreAuthorize).
- Database: Oracle. Each microservice owns its schema with JPA entities.
- Messaging: Kafka used for events like transactions and status updates.
- Files/Uploads: Stored on disk (e.g., for KYC and Self-Service documents).
- Observability: Actuator health, optional Zipkin, optional Prometheus/Grafana.
- AI Orchestrator: Optional automation for admin queues (KYC, Loans, Salary, Cards, Self-Service) using:
  - OCR (Apache Tika) to read uploaded docs
  - LLM (Ollama) to make nuanced decisions when rules are ambiguous
  - OAuth2 client-credentials for secure admin calls
  - Modes: OFF, DRY_RUN (log-only), AUTO (execute decisions)

Key Gateway routes (api-gateway/src/main/resources/application.yaml):
- /auth/** → user-service
- /accounts/** → account-service
- /transactions/** → transaction-service
- /loans/** → loan-service
- /cards/** → credit-card-service
- /notifications/** → notification-service

Admin UI additionally calls the Orchestrator directly at http://localhost:9101 (not behind gateway by default).

--------------------------------------------------------------------------------

# 2) API Gateway

File: api-gateway/api-gateway/src/main/resources/application.yaml

What it does:
- CORS: Allows Angular apps at 4200 (customer) and 4300 (admin).
- Discovery locator: Maps lb://user-service, etc., via Eureka.
- Rewrite filters: Preserve service base paths (/auth, /accounts, etc.).
- WebFlux Gateway with DEBUG logs enabled for routing troubleshooting.

Used libraries:
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka client
- Spring Security (reactive auto-config excluded; gateway is open in dev)

Why this design:
- One URL for the UI(s)
- Centralized CORS and routing
- Extensible filters for auth, rate limiting, etc. if needed later

--------------------------------------------------------------------------------

# 3) Discovery Server (Eureka)

Folder: discovery-server/

What it does:
- Services register themselves
- Gateway routes to them by service ID
- Avoids hardcoding host/ports and enables horizontal scaling

--------------------------------------------------------------------------------

# 4) AI Orchestrator (Detailed)

Paths:
- Config: ai-orchestrator/ai-orchestrator/src/main/resources/application.yaml
- Controller: .../web/AgentController.java
- Workflows: .../workflow/*.java (KycWorkflow, LoanWorkflow, SalaryWorkflow, CardWorkflow, SelfServiceWorkflow)
- AI/OCR: .../ai/* (TikaOcrService, NoopOcrService, LlmAiReasoner, NoopAiReasoner)
- Security/Feign: .../config/* (SecurityConfig, FeignOAuth2Config, CorsConfig)
- Services: .../service/* (AgentStateService, OrchestratorService, QueueMetricsService, AuditService)

Runtime configuration (application.yaml):
- server.port: 9101
- orchestrator.agent: enable/disable, mode, workflow toggles, polling interval
- orchestrator.integrations: base URLs for gateway/services
- orchestrator.ai: provider (noop|ollama), model name (e.g., llama3.2:3b), temperature, timeout
- Spring Security OAuth2 client (client-credentials) for admin calls via Feign

Exposed endpoints (AgentController):
- GET /agent/status
  - Returns: enabled state, mode (OFF|DRY_RUN|AUTO), workflow toggles, polling enabled/interval, last run time, and queue sizes snapshot.
- PUT /agent/toggle
  - Request: JSON allowing changes to enabled, mode, per-workflow flags (kyc, loans, salary, cards, selfService), polling on/off and interval.
  - Response: same as /status after applying changes.
- POST /agent/run-now
  - Trigger an immediate background run.

Core building blocks:
- AgentStateService: In-memory state for flags, mode, polling cadence.
- OrchestratorService: Coordinates polling and invoking individual workflows.
- QueueMetricsService: Exposes per-queue backlogs to UI.
- FeignOAuth2Config: Adds OAuth2 token to outbound Feign calls using the configured client registration.
- OcrService (Apache Tika): Extracts text from uploaded documents (PDF/images).
- AiReasoner: Pluggable reasoner; Noop (baseline) or LLM via Ollama.

KYC Workflow (KycWorkflow.java) — step-by-step:
1) Pull SUBMITTED applications: kycAdminClient.listByStatus(SUBMITTED)
2) Validate formats:
   - Aadhaar: 12 digits (^\d{12}$)
   - PAN: ABCDE1234F (^[A-Z]{5}\d{4}[A-Z]$)
   - Address completeness
   - At least one document uploaded
3) If formats ok:
   - Download docs (admin stream), OCR content with Tika
   - Look for corroboration: PAN text match, last 4 digits of Aadhaar, address fragments
   - If corroborated → APPROVE with a comment
   - Else → Ask AiReasoner with structured inputs (aadhaar, pan, address fields, doc text)
     - If APPROVE → approve with AI comment
     - Else → REJECT with reason (AI or fallback “not corroborated”)
4) Modes:
   - DRY_RUN: only log decisions
   - AUTO: call review endpoint to approve/reject
   - OFF: skip processing
5) Metrics: queue size set for UI dashboard

Other Workflows (Loan/Salary/Card/Self-Service):
- Follow a similar pattern: list items in SUBMITTED (or equivalent) status, apply business rules, optionally OCR/AI, then approve/reject via admin endpoints if AUTO.
- Admin endpoints exist in corresponding services:
  - Loans: PUT /loans/{loanId}/approve | /reject
  - Salary accounts: PUT /accounts/salary/applications/{id}/review
  - Cards: PUT /cards/applications/{id}/review
  - Self-service requests: /self-service/admin/requests/{requestId}/approve or /reject
- Use QueueMetricsService to reflect backlogs per workflow to UI.

Admin Dashboard integration:
- Angular Admin calls /agent/status to render the current state and queue metrics.
- It uses /agent/toggle to enable/disable DRY_RUN/AUTO and granular workflow flags.
- /agent/run-now can be used for on-demand runs (e.g., a daily batch triggered manually).

Security:
- For dev, SecurityConfig may be permissive. In production: do not expose orchestrator publicly; protect behind gateway and require admin tokens, or run it internally on a private network.

--------------------------------------------------------------------------------

# 5) User Microservice (Detailed)

Controllers:
- /auth (UserController)
- /auth/kyc (KycController)
- /auth (ProfilePhotoController for photos)

Key features:
- Register and manage user profiles
- KYC application submission (multipart with documents)
- Admin KYC review and secure document streaming
- Profile photos (upload/stream)
- Kafka producer for KYC status updates

Endpoints and behavior:
- POST /auth/register
  - Who: permitAll (no token)
  - Request: JSON with basic user info
  - Validations: DTO-level (e.g., email format)
  - Effect: Creates a user (and may sync with Keycloak admin client)
  - Side effects: optional notifications
- GET /auth/user/{userId}
  - Who: isAuthenticated()
  - Returns: user profile
- PUT /auth/user/{userId}
  - Who: isAuthenticated() and owner (#userId == authentication.principal.subject) or hasRole('ADMIN')
  - Request: profile update fields
- PUT /auth/user/{userId}/kyc-status
  - Who: ADMIN
  - Request: enum status; used by admin tooling/Orchestrator as needed
  - Side effect: publishes Kafka “kyc-status-events”
- DELETE /auth/user/{userId}
  - Who: ADMIN
  - Effect: deletes/deactivates user
- GET /auth/profile
  - Who: isAuthenticated()
  - Returns: current user’s profile derived from token subject
- GET /auth/users
  - Who: ADMIN
  - Returns: list of users

KYC endpoints:
- POST /auth/kyc/applications (multipart/form-data)
  - Who: isAuthenticated()
  - Request: form fields (aadhaar, pan, address…) + documents (PDF/image)
  - Storage: files go to uploads/; DB stores metadata and relative paths
- GET /auth/kyc/applications
  - Who: ADMIN
  - Returns: list filtered by status (default SUBMITTED)
- GET /auth/kyc/applications/{applicationId}
  - Who: ADMIN
  - Returns: application details
- PUT /auth/kyc/applications/{applicationId}/review
  - Who: ADMIN
  - Request: decision (APPROVED|REJECTED) + comment
  - Side effects: may publish Kafka event, notify user
- GET /auth/kyc/applications/{applicationId}/documents
  - Who: ADMIN
  - Produces: OCTET_STREAM for secure document streaming (path validation in service layer)

Profile Photo:
- POST /auth/user/{userId}/photo
- GET /auth/user/{userId}/photo
  - Security: owner-or-admin via SpEL checks

Domain model:
- User (JPA @Entity)
- KycApplication (JPA @Entity: fields for aadhaar, pan, address, document paths, status)

Integrations:
- KafkaTemplate<String, KycStatusUpdatedEvent> to publish “kyc-status-events”
- Keycloak admin client for account provisioning (internal)
- Invoked by Orchestrator KYC admin client for reviews and document streaming

--------------------------------------------------------------------------------

# 6) Account Microservice (Detailed)

Controllers:
- /accounts (AccountController)
- /accounts/salary/applications (SalaryAccountApplicationController)

What it does:
- Account lifecycle (create/get/update/delete)
- Money operations (deposit/withdraw) from the account side
- Salary account applications with documents and admin review

Endpoints (AccountController):
- POST /accounts/create
  - Create a new account for a user
- GET /accounts/user/{userId}
  - List accounts belonging to a userId
- GET /accounts/{accountId}
  - Get account by ID
- GET /accounts/number/{accountNumber}
  - Lookup by account number
- PUT /accounts/{accountId}
  - Update account status/metadata
- DELETE /accounts/{accountId}?otpCode=...
  - OTP-gated deletion (verify via OTP service)
- POST /accounts/{accountId}/deposit
- POST /accounts/{accountId}/withdraw
  - Validations: DTO @Valid; service layer checks balances and status

Salary Applications (SalaryAccountApplicationController):
- POST /accounts/salary/applications (JSON)
- POST /accounts/salary/applications (multipart/form-data)
  - Both create a salary account application with documents
- GET /accounts/salary/applications (ADMIN)
  - List applications (supports status filter)
- GET /accounts/salary/applications/mine
  - Current user’s applications
- GET /accounts/salary/applications/{id} (ADMIN)
- PUT /accounts/salary/applications/{id}/review (ADMIN)
  - Approve/Reject with comments
- GET /accounts/salary/applications/{id}/documents (ADMIN)
  - Secure streaming of uploaded files

Domain model:
- Account (JPA @Entity)
- SalaryAccountApplication (JPA @Entity)

Integrations (Feign):
- OtpServiceClient: verify OTP (and public verify for email flows)
- NotificationServiceClient: email/sms notifications
- TransactionServiceClient: record fines/internal bookkeeping if needed
- UserServiceClient: profile lookups where necessary

Security:
- Admin-only review endpoints
- OTP required for destructive operations (delete)
- Ownership enforced at controller/service level

--------------------------------------------------------------------------------

# 7) Transaction Service (Detailed)

Controller: /transactions (TransactionController.java)

What it does:
- Official ledger of money movements
- Supports deposit, withdraw, transfer, internal debit, debit-card withdraw
- Exposes transaction history
- Statement generation: OTP verification + password-protected PDF via email

Endpoints:
- POST /transactions/deposit
- POST /transactions/withdraw
- POST /transactions/transfer
  - Validations: @Valid DTOs; service throws domain exceptions handled by GlobalExceptionHandler
- POST /transactions/fine
  - Internal fine recording
- POST /transactions/internal/debit
  - For microservice-to-microservice internal fees (e.g., card issuance fee)
  - JWT required; no OTP
- POST /transactions/debit-card/withdraw
  - Validates debit card with CreditCardService and OTP as needed
- GET /transactions/account/{accountId}
  - Returns list; 204 NO_CONTENT if empty (by current implementation)
- GET /transactions/{transactionId}
  - Returns one or 404

Statements (2-step with OTP and PDF):
1) POST /transactions/statements/initiate
   - Choose recipient email (from request or fallback to user profile fetched via UserServiceClient)
   - Call OTP public generate with contextId = "STATEMENT:{accountId}:{from}_{to}"
   - Returns requestId and expiry
2) POST /transactions/statements/verify
   - Verify OTP public with same contextId
   - Fetch transactions in date range
   - Build password = FIRST4(UPPERCASE firstName) + YEAR(dob); e.g., ABCD2003; pad with X if short
   - Generate PDF with Apache PDFBox:
     - Title, period, user details, account details
     - Transaction table (date/time, type, amount, status, from, to, txnId)
   - Protect PDF with StandardProtectionPolicy (128-bit), using the user password
   - Send email with JavaMailSender (tries to set From = SMTP username)
   - On MailException, gracefully notify via NotificationService and return 202 ACCEPTED instead of 500

Domain model:
- Transaction (JPA @Entity)

Integrations (Feign):
- AccountServiceClient: account lookups for balance and details
- UserServiceClient: user profile email/name/DOB for statements
- CreditCardServiceClient: debit-card transaction validation
- LoanServiceClient: related loan lookups if needed
- NotificationServiceClient: email/sms fallbacks/notifications
- OtpServiceClient: OTP generate/verify (authenticated and public)

Messaging:
- KafkaTemplate<String, TransactionCompletedEvent>: publishes to "transaction-events"
- Resilience4j:
  - CircuitBreaker(name="kafkaNotificationPublisher", fallbackMethod=publishTransactionCompletedEventFallback)
  - Retry for transient Kafka failures
- Kafka config in application.yaml (bootstrap-servers, producer JSON serializer)

Exceptions:
- GlobalExceptionHandler (@ControllerAdvice) centralizes error responses
- Domain exceptions: AccountNotFound, InsufficientFunds, InvalidTransaction, TransactionProcessing

--------------------------------------------------------------------------------

# 8) Loan Service (Detailed)

Controller: /loans (LoanController)

What it does:
- Loan lifecycle management: apply, view by id or user, list all (admin), approve/reject (admin), compute EMI

Endpoints:
- POST /loans/apply
- GET /loans/{loanId}
- GET /loans/user/{userId}
- GET /loans
- PUT /loans/{loanId}/approve
- PUT /loans/{loanId}/reject (supports structured reason request body)
- GET /loans/{loanId}/emi

Security:
- Intended @PreAuthorize expressions for role/ownership (some commented for dev); re-enable for prod
- Approvals/rejections are ADMIN-only

Domain model:
- Loan (JPA @Entity)

Integrations:
- UserClient for user profile linkage
- OtpServiceClient where OTP is required in specific flows
- KafkaTemplate<String, LoanStatusUpdatedEvent> to publish "loan-status-events"

AI Orchestrator:
- LoanWorkflow polls SUBMITTED loans, applies rules/AI, and auto-approves/rejects when AUTO

--------------------------------------------------------------------------------

# 9) Credit Card Service (Detailed)

Controllers:
- CreditCardController (/cards)
- CardsController (/cards)

What it does:
- Issue and manage credit/debit cards
- Admin review of card applications
- OTP-gated sensitive operations (block/unblock, reveal PAN, regenerate CVV)
- Debit-card validation for withdrawals (used by TransactionService)
- Fees query

Key endpoints:
- POST /cards/issue
- GET /cards/user/{userId}
- GET /cards/{cardId}
- PUT /cards/{cardId}/block?otpCode=...
- PUT /cards/{cardId}/unblock?otpCode=...
- PUT /cards/{cardId}/limit (update limit)
- GET /cards/{cardId}/transactions
- Debit card validation:
  - POST /cards/debit/validate-transaction
- Sensitive (OTP-gated):
  - POST /cards/{id}/reveal-pan
  - POST /cards/{id}/regenerate-cvv
- Applications (CardsController):
  - POST /cards/applications
  - GET /cards/applications/mine?userId=...
  - Admin: GET /cards/applications
  - Admin: GET /cards/applications/{id}
  - Admin: PUT /cards/applications/{id}/review
- Fees:
  - GET /cards/fees?accountType=SAVINGS|CURRENT&kind=DEBIT|CREDIT

Security:
- Admin endpoints protected by @PreAuthorize("hasRole('ADMIN')")
- OTP required for sensitive operations

Domain model:
- Card (JPA @Entity)
- CardApplication (JPA @Entity)
- CreditCard (JPA @Entity)

Integrations (Feign):
- InternalTransactionClient → /transactions/internal/debit for internal debits (e.g., issuance fee)
- NotificationServiceClient → send-email/send-sms
- OtpServiceClient → verify codes
- AccountServiceClient → account relationships/limits
- UserServiceClient → ownership and profile details
- TransactionServiceProxy → query transactions by cardId

AI Orchestrator:
- CardWorkflow for admin automation of application reviews

--------------------------------------------------------------------------------

# 10) Notification Service (Detailed)

Controller: /notifications (NotificationController)
- POST /notifications/send-email
- POST /notifications/send-sms
- GET /notifications/user/{userId}

What it does:
- Centralized email/SMS service
- Also consumes Kafka events to trigger notifications automatically

Kafka Consumers:
- Listeners:
  - "transaction-events"
  - "kyc-status-events"
  - "loan-status-events"
- Config (NotificationService/src/main/resources/application.yaml):
  - bootstrap-servers
  - consumer group (notification-service-group)
  - ErrorHandlingDeserializer + JsonDeserializer
  - spring.json.trusted.packages
- Factory (KafkaConsumerConfig.java): Configures listener container, deserializers, and concurrency if needed

Domain model:
- Notification (JPA @Entity) to store notification history/meta

--------------------------------------------------------------------------------

# 11) OTP Service (Detailed)

Controller: /otp (OtpController)
- POST /otp/generate (authenticated)
- POST /otp/verify (authenticated)
- POST /otp/public/generate (public)
- POST /otp/public/verify (public)

Purpose:
- Generate/verify OTP codes for multi-step flows (statements, PAN reveal, contact change, account delete)
- Public endpoints are for pre-login or email-change flows

Domain model:
- OtpCode (JPA @Entity with indexes on context/user)

Integrations:
- NotificationClient → send-email to deliver OTP codes

Security:
- Mix of authenticated and public endpoints to support different UX flows
- Use contextId to bind OTP to a specific business operation (e.g., STATEMENT:{acct}:{from}_{to})

--------------------------------------------------------------------------------

# 12) Self-Service Microservice (Detailed)

Controllers:
- SelfServiceRequestController (/self-service/requests)
- AdminRequestsController (/self-service/admin/requests)
- ContactController (/self-service/contact)
- NomineeController (/self-service/nominees)

What it does:
- Let users submit requests with documents for admin review
- Update contact (email/phone) via OTP verification
- Manage nominees

Endpoints:
- Requests (user-facing):
  - POST /self-service/requests (multipart/form-data; docs mandatory)
    - Security: isAuthenticated() and #userId == authentication.name or ADMIN
  - GET /self-service/requests/mine
    - Security: isAuthenticated() and #userId == authentication.name or ADMIN
- Requests (admin-facing):
  - GET /self-service/requests (ADMIN; status filter)
  - GET /self-service/requests/{id} (ADMIN)
  - PUT /self-service/requests/{id}/review (ADMIN; approve/reject)
  - GET /self-service/requests/{id}/documents (ADMIN; OCTET_STREAM)
- AdminRequestsController (alternate admin path):
  - GET /self-service/admin/requests (ADMIN)
  - GET /self-service/admin/requests/{requestId} (ADMIN)
  - POST /self-service/admin/requests/{requestId}/approve (ADMIN)
  - POST /self-service/admin/requests/{requestId}/reject (ADMIN)
  - GET /self-service/admin/requests/{requestId}/documents/** (ADMIN)
- Contact:
  - POST /self-service/contact/email/initiate
  - POST /self-service/contact/email/verify
  - POST /self-service/contact/phone/initiate
  - POST /self-service/contact/phone/verify
  - Pattern: initiate uses public OTP to new contact; verify applies update
- Nominees:
  - POST /self-service/nominees
  - GET /self-service/nominees?userId=...
  - PUT /self-service/nominees/{id}
  - DELETE /self-service/nominees/{id}
  - Security: ownership or ADMIN via SpEL

Domain model:
- SelfServiceRequest (JPA @Entity)
- Nominee (JPA @Entity)

Integrations:
- UserServiceClient
- OtpServiceClient (both authenticated and public)
- NotificationServiceClient

AI Orchestrator:
- SelfServiceWorkflow to automate admin queue

--------------------------------------------------------------------------------

# 13) Angular Frontends (Detailed)

A) Customer App (Angular Frontend/banking-frontend)
- SPA for end-users: registration, profile, accounts, transactions, loans, cards, self-service
- Calls Gateway routes (/auth, /accounts, /transactions, /loans, /cards, /notifications)
- OIDC Keycloak login (Authorization Code Flow)
- Recommended patterns:
  - HTTP interceptors for tokens
  - Route guards for protected views
  - Centralized error handling with user-friendly toasts

B) Admin Dashboard (Angular Frontend/banking-admin-dashboard)
- environment.ts:
  - apiUrl: http://localhost:9010 (Gateway)
  - orchestratorUrl: http://localhost:9101 (AI Orchestrator)
  - keycloak: issuer, clientId=bank-admin-frontend, redirectUri=http://localhost:4300
- Features:
  - View and review KYC, loan, card, salary, and self-service queues
  - Toggle Orchestrator mode (OFF/DRY_RUN/AUTO), per-workflow flags, polling interval
  - See live queue sizes and last run in dashboard
- agent.service.ts:
  - Wraps calls to GET /agent/status, PUT /agent/toggle, POST /agent/run-now
- CORS: Gateway allows http://localhost:4300

--------------------------------------------------------------------------------

# 14) Security Model and Expressions (Quick Reference)

- Keycloak issues JWT tokens.
- Services use @PreAuthorize with SpEL:
  - isAuthenticated()
  - hasRole('ADMIN')
  - Ownership checks:
    - "#userId == authentication.principal.subject" (subject = userId)
    - "#userId == authentication.name"
- OTP adds a second factor to sensitive flows:
  - Statements, account deletion, PAN reveal, CVV regenerate, contact updates
- For production:
  - Enforce resource-server validation at each microservice (or protect everything behind a JWT-validating gateway)
  - Re-enable any commented @PreAuthorize annotations
  - Limit public OTP endpoints to least privilege and tight rate limiting

--------------------------------------------------------------------------------

# 15) Messaging (Kafka) — What and Why

Producers:
- Transaction Service → "transaction-events" (TransactionCompletedEvent)
- User Microservice → "kyc-status-events" (KycStatusUpdatedEvent)
- Loan Service → "loan-status-events" (LoanStatusUpdatedEvent)

Consumer:
- Notification Service listens on these topics to send user notifications.

Implementation highlights:
- Spring Kafka with JSON serializer/deserializer
- Resilience4j CircuitBreaker/Retry around Kafka publishing (Transaction Service)
- ErrorHandlingDeserializer on consumers for robust processing

Operational reminders:
- Create topics before running locally
- Tune consumer group IDs and concurrency for throughput

--------------------------------------------------------------------------------

# 16) Persistence (JPA Entities by Service)

- UserMicroservice:
  - User
  - KycApplication
- AccountMicroservice:
  - Account
  - SalaryAccountApplication
- TransactionService:
  - Transaction
- Loan Service:
  - Loan
- CreditCardService:
  - Card
  - CardApplication
  - CreditCard
- NotificationService:
  - Notification
- OTP Service:
  - OtpCode
- SelfService:
  - SelfServiceRequest
  - Nominee

General pattern:
- Repositories per aggregate
- DTOs with @Valid for inbound API
- Service layer to enforce business rules and throw domain exceptions

--------------------------------------------------------------------------------

# 17) End-to-End Workflow Scenarios (Step-by-Step)

A) KYC submission → Admin review → Automation (Orchestrator)
1) User uploads KYC with documents (User Service).
2) Admin queue shows SUBMITTED apps.
3) Orchestrator (AUTO):
   - Validates PAN/Aadhaar formats and address
   - OCRs docs with Tika and tries to corroborate
   - If ambiguous, asks LLM (Ollama) with structured inputs
   - Approves/Rejects via User Service admin endpoint with a comment
4) User is notified via Notification Service (email/SMS) and Kafka event consumers.

B) Salary Account Application (with documents)
1) User submits salary application (JSON or multipart) in Account Service.
2) Admin reviews via /accounts/salary/applications/{id}/review.
3) Orchestrator can automate (SalaryWorkflow) using similar SUBMITTED → approve/reject logic.
4) Documents are retrievable by admin via secure streaming.

C) Debit Card Withdraw (ATM-like)
1) User initiates withdraw via Transaction Service.
2) Transaction Service calls Credit Card Service /cards/debit/validate-transaction.
3) OTP verification as needed.
4) Transaction recorded; Kafka event published; Notification service sends confirmation.

D) Monthly Statement (OTP + password PDF)
1) Initiate → OTP sent to email (public flow).
2) Verify → generate PDF via PDFBox; protect with password FIRST4NAME+YYYY; email it.
3) If email fails, fallback notification is sent via Notification Service.

E) Contact Update (email or phone)
1) Initiate → send OTP (public) for new email or authenticated for phone.
2) Verify → update user profile in User Service.
3) Notification of change is sent.

F) Credit Card Issuance Fee (internal debit)
1) Card issuance triggers InternalTransactionClient → /transactions/internal/debit.
2) Transaction is recorded against account; Kafka event triggers post-notifications.

--------------------------------------------------------------------------------

# 18) Libraries and Tools by Use-Case

- HTTP clients between services: OpenFeign
  - OAuth2 client-credentials (Orchestrator’s FeignOAuth2Config)
  - Resilience4j CircuitBreaker/Retry on Feign calls (e.g., Account/Loan/User/Notification)
- OTP: Dedicated OTP service with public and authenticated endpoints
- PDFs: Apache PDFBox for statement PDFs and password protection
- OCR: Apache Tika to extract text from uploaded docs (for KYC corroboration)
- LLM: Ollama (model e.g., llama3.2:3b) — AiReasoner to decide approve/reject with reason
- Messaging: Spring Kafka producers/consumers for async notifications
- Security: Spring Security + Keycloak OIDC; method-level @PreAuthorize
- Routing/CORS: Spring Cloud Gateway
- Discovery: Eureka

--------------------------------------------------------------------------------

# 19) Deployment and Configuration Notes

- Externalize secrets:
  - OAuth2 client secret for orchestrator
  - SMTP credentials for JavaMailSender
- Databases:
  - Create schemas per service and adjust spring.datasource.* accordingly
- Kafka:
  - Ensure topics exist; align topic names in code and infra
- Gateway CORS:
  - Update allowedOrigins for deployed frontend URLs
- Observability:
  - Enable Zipkin in management.tracing.export.zipkin
  - Add Prometheus scrape configs for each service’s actuator

--------------------------------------------------------------------------------

# 20) Directory Map (Top-Level)

- AccountMicroservice/
- CreditCardService/
- TransactionService/
- UserMicroservice/
- loan-service/
- NotificationService/
- otp-service/
- SelfService/
- api-gateway/
- discovery-server/
- config-server/
- ai-orchestrator/
- Angular Frontend/
  - banking-frontend/
  - banking-admin-dashboard/

--------------------------------------------------------------------------------

# 21) Practical Checklists (Per Role)

Developers:
- Add a new endpoint? Define DTOs with @Valid, service logic, exceptions, and @PreAuthorize.
- Calling another service? Create a Feign client + Resilience4j annotations.
- Sensitive operation? Add OTP step + Notification.
- Documents? Store under uploads/ and stream securely to admins.
- Automation candidate? Add a *Workflow to Orchestrator and expose an admin review endpoint.

Operators:
- Confirm Gateway routes match service base paths.
- Verify Eureka shows all running services.
- Check Kafka topics, consumer groups, and lag.
- Monitor Orchestrator queues and modes via Admin Dashboard.

Security:
- Revisit commented @PreAuthorize blocks; enforce resource server validation.
- Move secrets to env vars and rotate regularly.
- Lock down public OTP endpoints with rate limiting, IP allowlists if needed.

--------------------------------------------------------------------------------

This document is based on the current repository code: controllers, Feign clients, application configs, OCR/AI workflow classes, and Kafka usage. Update it as endpoints or behaviors evolve to keep it a reliable single source of truth.
