# OmniBank — End-to-End Delivery Plan (Updated Master Plan)

Status: Active  
Branching: feature branches + PRs (current: faizal03hussain-feature-branch)  
Profiles: dev-open (default), kafka (async/evented), secure (later)  
Environments: local-first (no Docker), later staging/prod-ready

This plan replaces the previous Next Plan and defines a complete, seamless path to deliver all domains: Core/Payments, Lending, Cards, Investments, Admin BFF, Reporting, Security, Observability, and (later) AI integrations. It is implementation-ready, precise, and avoids breaking services by using feature flags, backward-compatible contracts, and a consistent idempotent/event-driven architecture.

---

## 0. Guiding Principles and Engineering Golden Paths

- Architecture: Microservices, event-driven with Kafka, API-first, idempotent consumers, consistent event envelope.
- Feature Flags: All risky/optional paths behind properties; services must be runnable without Kafka (dev-open) and with Kafka (kafka).
- Profiles:
  - dev-open: in-memory/H2 + logging events (no external brokers)
  - kafka: enables Kafka producers/consumers and disables dev shortcuts
  - secure: OAuth2/Keycloak enforcement
- Contracts:
  - HTTP: URI versioned /api/v1/..., consistent error shape, X-Correlation-Id propagation end-to-end
  - Events: Envelope { topic, type, timestamp, correlationId, payload }, version-friendly payloads
- Idempotency:
  - External write APIs expect Idempotency-Key (gateway, disbursal, payments)
  - Consumers use marker tables keyed on natural identifiers (e.g., txId, paymentUuid) to avoid reprocessing
- Testing Pyramid: unit > service integration > contract tests > E2E (Postman)
- Non-Functional: clear SLOs, timeouts, circuit breakers, retries, bulkheads; structured logging; metrics; tracing (later)
- Postman: Keep OmniBank.local collection/environment current with each feature

---

## 1. Current Baseline (Completed/In Place)

- Payments async over Kafka end-to-end (local):
  - payment-gateway publishes PaymentApprovedForProcessing
  - ledger consumes, posts double-entry, publishes TransactionPosted
  - account-management consumes TransactionPosted idempotently and updates balances
- Payments hardening:
  - Idempotency at gateway; dev balance adjust disabled under kafka; timeouts and CB/Retry on outbound clients
- Lending MVP-I:
  - loan-origination: in-memory app lifecycle; publishes LoanApplicationSubmitted / LoanApproved; kafka profile available
  - loan-management: creates loan account on LoanApproved; query APIs for loans by customer and account
- Lending MVP-II (in progress, partially delivered):
  - loan-origination: orchestrates disbursal on APPROVED via payment-gateway; publishes LoanDisbursed
  - loan-management: generates schedule; scaffolding to apply EMIs idempotently (via ledger TransactionPosted)

---

## 2. Immediate Next Steps (Fast Track)

2.1 Payments Hardening (Complete polish)
- Add Resilience4j Bulkhead to all outbound clients (gateway + origination):
  - Configure thread/semaphore isolation per client with sensible limits
  - Verify timeouts match bulkhead and retry windows
- Tests:
  - payment-gateway: unit tests for FRAUD BLOCK/CHALLENGE orchestration branches
  - ledger: unit tests for double-entry invariants and rounding cases
  - account-management: consumer idempotency test (existsByTransactionId flows)
- Load Test (local baseline):
  - JMeter/Gatling-lite to validate p95/p99 on gateway under kafka profile
- Postman:
  - Add negative tests (insufficient funds, inactive beneficiary, fraud-block)

2.2 Lending MVP-II Completion
- loan-origination:
  - Beneficiary policy: add secure internal flag/property to bypass beneficiary check for bank-to-customer disbursal only (later in gateway) OR pre-create bank-to-customer mapping (SAFE DEFAULT: keep beneficiary requirement, document a bank-disbursal bypass to be added in secure profile)
  - Ensure disbursal idempotency keyed by applicationId cannot double debit on retries
- loan-management:
  - Complete EMI application path:
    - Replace loan-account heuristic with metadata mapping when available (see 4.3 ledger metadata)
    - Ensure partial payment handling improves schedule logic (carry-forward pending principal and mark payment proportionally in next iterations)
- Postman:
  - Add “Loan Schedule” retrieval
  - Add “Apply EMI (dev)” to simulate repayments

Acceptance:
- Payments pass E2E in kafka profile and dev-open without breakage
- Lending: Approve -> disburse -> view updated balances -> view loan and schedule -> simulate EMI debit via ledger event; balances and schedule reflect payment

---

## 3. Lending — Productization Phase

3.1 loan-origination (persist and enrichment)
- Persistence: swap in-memory store to Mongo (dev-friendly profile retains in-memory)
  - application document schema with history/attachments placeholder
- Credit Scoring Integration (mock to real flow)
  - call credit-scoring-service for applicantId, attach result to underwriting history
  - guard with timeouts/CB/Retry/Bulkhead
- Event evolution:
  - LoanApproved payload includes tenure/months and EMI characteristics (so schedule can be generated consistently)
- Disbursal:
  - Add “internal-disbursal” flag to bypass beneficiary checks in gateway (secure profile only); dev/kafka keep current behavior
  - Error handling: disbursal failure produces 409 with details; no LoanDisbursed published; allow re-attempt with same Idempotency-Key

3.2 loan-management
- Schedule:
  - Generate schedule from approved tenure and rate; support custom start date
  - Store amortization schedule; expose GET /api/v1/loans/{loanAccount}/schedule (already added)
- EMI Application:
  - Introduce mapping:
    - ledger TransactionPosted payload includes “metadata.loanAccountNumber” (see 4.3)
    - consumer uses metadata to apply EMI instead of account heuristic
  - Partial payments:
    - Apply payment against interest first, then principal; split schedule entry if partial; track arrears
  - Close loan when balance <= threshold; publish LoanClosed (loan.management.events, later)
- APIs:
  - GET /api/v1/loans/{loanAccountNumber}
  - GET /api/v1/loans/customers/{customerId}
  - GET /api/v1/loans/{loanAccountNumber}/schedule

Acceptance:
- End-to-end loan: create -> approve (+scoring) -> disburse -> schedule generated -> apply EMIs via ledger -> balances/schedule consistent

---

## 4. Core/Platform Enhancements

4.1 Security (Phase A local-open + Phase B secure)
- Phase A (dev-open retained):
  - Consistent CORS, minimal security config for local development
- Phase B (secure profile):
  - Keycloak realm, clients, roles/scopes
  - API Gateway validates JWTs, enforces RBAC (admin/customer roles)
  - Resource servers on all services (Spring Security + OAuth2)
  - Secrets via env/properties (no hardcoded secrets)
- Admin flows protected with admin scopes

4.2 Observability
- Logging:
  - Structured JSON logs; correlationId propagation already in place
- Metrics:
  - Micrometer/Prometheus enabled for all services
  - Dashboards:
    - Gateway latency, error rates; business KPIs (payments/min, approvals/min)
    - Lending dashboards (approvals/day, outstanding balance, EMIs due/paid)
- Tracing (later when OTEL agent available):
  - Configure OTLP exporter; Jaeger in dev-opt in (disabled by default)

4.3 Eventing/Envelope consistency & metadata
- Standardize envelope object across all publishers (already consistent)
- Metadata:
  - Augment relevant events with metadata fields:
    - TransactionPosted.payload.metadata.loanAccountNumber (when applicable)
    - PaymentApprovedForProcessing.payload.metadata.intent (e.g., DISBURSAL|TRANSFER)
- Topic naming and retention:
  - payment.events, ledger.events, loan.origination.events, account.events (if needed)
  - Document retention and compaction policies (later infra)

4.4 Resilience & Performance
- Resilience4j: unify CB/Retry/Bulkhead timeouts and thresholds per client
- Connection pooling tuning (Hikari) for H2/Oracle later
- Load test suites: gateway and card authorization paths (later)

---

## 5. Cards Domain (MVP)

5.1 card-issuance-service (dev-local)
- In-memory workflow: SUBMITTED -> ELIGIBILITY_CHECKED -> APPROVED
- Publish CardApplicationApproved (card.issuance.events)
- API: submit application; get status

5.2 card-management-service
- Domain: Card master (hash, last4, status)
- APIs:
  - GET /api/v1/cards/{cardId}
  - POST /api/v1/cards/{cardId}/activate
  - POST /api/v1/cards/{cardId}/status (BLOCK/UNBLOCK)
  - POST /api/v1/cards/{cardId}/limits
- Events: CardCreated, CardStatusUpdated, CardLimitsChanged
- Consumes CardApplicationApproved to create card

5.3 card-authorization-service (MVP)
- Internal auth endpoint: POST /api/v1/internal/card/authorize
- Cache (in-memory; Redis later) of card status/limits
- Fraud sync call to fraud-detection-service (mock -> AI later)
- SLA target: p99 < 100ms (local)
- Subscribes to card-management events to refresh cache

Postman:
- Submit card app -> approve -> create card -> activate -> set limits -> test authorization decisions

Acceptance:
- Card lifecycle with self-service APIs; authorization returns APPROVE/DECLINE correctly

---

## 6. Investments Domain (MVP)

6.1 investment-onboarding-service (dev-local); publish InvestmentProfileActivated
6.2 product-catalog-service (H2 + cache)
- Seed product data; search/filter; daily NAV mock; events ProductNAVsUpdated

6.3 order-management-service (OMS)
- Validate investment profile
- Orchestrate payment debit via gateway; simulate exchange execution; publish InvestmentOrderExecuted

6.4 portfolio-management-service (H2)
- Consume InvestmentOrderExecuted; maintain holdings
- Revalue holdings on ProductNAVsUpdated

Postman:
- Activate profile -> search products -> place buy -> simulate execution -> view holdings

Acceptance:
- Buy flow functional; portfolio reflects holdings; valuations update on NAV

---

## 7. Admin BFF + Reporting

7.1 Admin BFF (Spring WebFlux)
- Aggregate customer 360 (profile/accounts/cards/loans/portfolio)
- Admin flows: freeze customer, block card, approve address changes
- RBAC via OAuth2 scopes
- APIs tailored for admin portal

7.2 Reporting service
- Phase A: Read-only OLTP joins/exports (CSV/PDF) from reporting DB (later)
- Phase B (later): CDC via Debezium/Kafka Connect to OLAP schema; high performance analytics

Acceptance:
- Two operational reports; 360 API functional with RBAC in secure profile

---

## 8. Security & Platform Hardening (Secure Profile)

- Configure Keycloak realm, clients, roles
- Gateway enforces JWT, roles/scopes; services as resource servers
- Rate limiting/throttling at gateway
- Secrets/config management, profile separation
- Scanning: SonarQube (SAST), OWASP deps
- Audit logging for sensitive actions

Acceptance:
- Secure profile blocks unauthenticated calls; roles enforced; audit trails created

---

## 9. Observability & SRE (Operationalization)

- OpenTelemetry tracing (optional in dev)
- Prometheus/Grafana dashboards (business + technical)
- Alerts configured (latency, error rates, SLO budgets)

Acceptance:
- E2E traces visible; dashboards confirm KPIs; alert workflow validated

---

## 10. AI Integrations (Later Phases)

- Fraud Detection AI Agent replaces heuristics in fraud-detection-service; returns explainable decision + score
- KYC/Address Verification AI Agents for onboarding/profile updates (OCR, liveness, AML/watchlists)
- Robo-Advisory: recommendations, allocation, rebalancing suggestions via events

Acceptance:
- AI-enhanced outcomes integrated; Postman demos updated

---

## 11. Developer Workflow, Runbooks, and Contracts

11.1 Developer workflow
- Profiles:
  - dev-open: fastest inner loop
  - kafka: async flows; disable dev shortcuts
  - secure: Keycloak/JWT required
- Properties pattern:
  - ${SERVICE}.eventPublisher=logging|kafka
  - account.kafka.enabled=true|false
  - ledger.events.topic=ledger.events
  - payment-gateway.devSyncPosting=true|false
  - loan-origination.integrations.disbursalSourceAccount=...
- Error Contract (HTTP):
  - { timestamp, status, error, message, correlationId, path }
- Event Envelope (Kafka/logging):
  - { topic, type, timestamp, correlationId, payload }
- Idempotency:
  - Gateway expects Idempotency-Key on POST write APIs
  - Marker tables in consumers: processedPayment, ledgerApplied, loanEmiApplied
- Postman:
  - Keep OmniBank.local collection/environment updated after each change set (IDs, base URLs, new endpoints)

11.2 Runbook (Kafka)
- Ensure $env:KAFKA_BOOTSTRAP_SERVERS set
- Start services with $env:SPRING_PROFILES_ACTIVE="kafka"
- Create topics if auto-create disabled:
  - payment.events, ledger.events, loan.origination.events
- Funding and beneficiary setup for disbursal

---

## 12. Sprint-Level Milestones (Indicative)

- Sprint A (Done): Payments async + idempotency + baseline tests
- Sprint B (Done/Partial): Lending MVP I (events + scoring mock), MVP II disbursal orchestration
- Sprint C: Lending MVP II (loan-management schedule + EMI application via ledger metadata; Postman drill)
- Sprint D: Cards MVP I (issuance/management)
- Sprint E: Cards MVP II (authorization + metrics)
- Sprint F: Investments MVP I (onboarding/catalog)
- Sprint G: Investments MVP II (OMS/portfolio)
- Sprint H: Admin BFF + Reporting Phase A
- Sprint I: Security & Observability hardening (secure profile, tracing, dashboards)
- Sprint J: AI Integrations Phase I

Definition of Done:
- Code merged with tests; coverage targets met
- Postman updated
- OpenAPI and event payload docs updated
- Feature behind property flag if applicable
- E2E verified locally in dev-open and kafka profiles

---

## 13. Risks and Mitigations

- Kafka unavailability in local: fallback logging publishers; graceful 409s on publish failure
- Disbursal beneficiary policy: keep strict in dev/kafka to avoid security gaps; add secure-flag bypass later
- Partial EMI complexity: MVP simplifies; plan includes follow-up for proportional application
- Security integration: staged in secure profile; services remain dev-open runnable
- Data migration to persistent stores (Oracle/Mongo) later; H2 retained for local dev

---

## 14. Next Actions (Actionable To-Do)

- Payments: Add Bulkhead configs and tests (BLOCK/CHALLENGE, ledger invariants, idempotent consumers)
- Lending: 
  - Finish schedule/EMI metadata mapping; improve partial EMI logic
  - Add Postman items for schedule + dev EMI apply
- Cards: Scaffold issuance/management; card-management events; tests
- Observability: Metrics dashboards; initial alerts
- Security: Prepare secure profile placeholders (properties, gateway filter stubs)
- CI: Add Sonar and coverage gates to CI; enforce quality gate on PRs

This plan ensures continued fast development without breaking services, with profiles and flags controlling feature exposure and reliable E2E through Kafka and REST. It keeps dev-open runnable at all times, and incrementally hardens toward secure, observable, and production-ready deployments.
