# OmniBank — Delivery Plan (vNext to Completion)

Status: Active
Profiles: dev-open (default), kafka (async/evented), secure (OAuth2/JWT)
Principles: Backward compatibility, feature flags, idempotency, consistent contracts, zero-downtime, CI quality gates

This plan replaces prior versions. It is prescriptive, end-to-end, and sequenced to ensure each increment is runnable and never breaks existing flows. All risky behavior is feature-flagged and/or profile-gated. The plan leverages our current baseline below.

---

## 1) Baseline (What Exists Today)

- Core services runnable locally (dev-open): customer-profile (8102), account-management (8103), beneficiary-management (8104), payment-gateway (8105), ledger (8106), loan-management (8122), card-issuance (8130), card-management (8131).
- Error envelope and correlationId echo standardized in most services.
- Cards
  - Dev-open issuance workflow (submit → eligibility → approve) with Logging publisher.
  - Dev-open card management (create dev card, activate, limits, block/unblock).
  - Kafka scaffolding: card-issuance Kafka publisher, card-management Kafka consumer for CardApplicationApproved with idempotency markers.
  - New query endpoints: get by issuance applicationId, list by customer.
  - Postman base flows; add-on for Kafka verification.
- Lending
  - Loan summary endpoint (outstanding principal, next EMI) + tests for schedule and EMI idempotency; schedule generation implemented.
- Angular Frontend (frontend/customer-angular on 5174)
  - Pages implemented: Cards, Payments, Beneficiaries, Loans, Accounts (balance/history).
  - CorrelationId interceptor, API client wired to dev-open ports.

---

## 2) Global Engineering Guardrails

- Architecture
  - Microservices communicating via HTTP + Kafka (kafka profile); logging publisher in dev-open.
  - Bounded contexts: payments, lending, cards, investments, admin/reporting, fraud/security.

- Profiles & Feature Flags
  - dev-open: H2/in-memory, logging event publishers, dev-only endpoints, permissive CORS.
  - kafka: enables Kafka producers/consumers; dev shortcuts disabled; retries and idempotency in consumers.
  - secure: JWT/OAuth2 (Keycloak or SAS); RBAC at BFF/gateway and resource servers.
  - All toggles via properties with documented defaults and migration runbooks.

- Contracts
  - HTTP error body: { timestamp, status, error, message, correlationId, path }.
  - Events: { topic, type, timestamp, correlationId, payload } with versioned payload schema.

- Idempotency
  - HTTP POST writes accept Idempotency-Key where relevant (e.g., payment-gateway).
  - Kafka consumers use marker tables keyed by natural identifiers (paymentUuid, transactionId, applicationId).
  - Retry-safe consumers; throw to trigger retry in Kafka profile.

- Non-Functional
  - Resilience4j: timeouts, retries, circuit breakers; thread pools bulkheading where needed.
  - Logging includes correlationId; metrics via Micrometer/Prometheus; traces via OTEL (later).

- CI/CD
  - Build/test/coverage gates enforced; OWASP dependency check; conventional PRs.

---

## 3) Environments & Runbooks

- Local dev without Docker; scripts/start-dev-open.ps1 launches core services (with -Kafka).
- Postman: OmniBank.local.postman_collection.json and OmniBank.cards.kafka.addon.json.
- Angular: frontend/customer-angular on 5174.
- Profiles: dev-open default; kafka toggled per service; secure later with Keycloak.

---

## 4) Roadmap by Tracks (Executable Increments)

Each track lists near-term increments (N), then medium-term (M), then long-term (L). Each increment ends in a runnable, tested state.

### Track A — Cards (Issuance/Management)

A1 (N): Kafka E2E completion (Current Sprint)
- Ensure end-to-end: issuance approves → publish CardApplicationApproved → management consumer auto-creates card idempotently.
- Merge Kafka card flow into main Postman collection (not only add-on).
- OpenAPI annotations for new endpoints; docs on topics and payload schema with versioning.
- Acceptance: Kafka profile E2E green; duplicate event replay yields 1 record; Postman and logs validated.

A2 (N): Validations and hardening
- Business rules: cannot activate BLOCKED card (done); limits validation and error shapes (done).
- Secure-profile-only rule: require reason for BLOCK; return 400 without reason in secure profile.
- Acceptance: dev-open unaffected; secure profile enforces reason; tests passing.

A3 (M): Observability for Cards
- Prometheus metrics: approvals/min, status-changes/min; structured logs with event type and correlationId.
- Grafana dashboards JSON shipped in repo.
- Acceptance: metrics counters visible; dashboards render; Cross-service correlation by correlationId.

A4 (L): Card event versioning, migrations, and persistence plan
- Draft event schema versioning (type+version), migration notes; optional outbox consideration later.
- Persistence migration (H2 -> Postgres) behind flag (optional stage).

### Track B — Core Payments

B1 (N): UI + Postman alignment
- Angular Payments: already implemented; finalize UX and form validation; ensure error envelope surfaced with correlationId.
- Postman flows mirror UI precisely; document Idempotency-Key usage.

B2 (N): Resilience & Idempotency verification
- Verify idempotency gate on payment-gateway (same Idempotency-Key yields same result).
- Standardize correlationId propagation across gateway → ledger → account-management (dev-open).
- Acceptance: tests and E2E passing; logs show correlationId chain.

B3 (M): Kafka pathway validation (optional)
- If gateway publishes processing events in kafka profile: verify ledger consumption & account updates.
- Acceptance: kafka profile shows equivalent results as dev-open sync path.

### Track C — Lending

C1 (N): Finalize endpoints & tests (done/ongoing)
- Loan summary endpoint (done) + test coverage.
- Ensure ledger event metadata for LOAN_EMI references loanAccountNumber in kafka profile (ledger config).
- Acceptance: schedule sum equals principal; EMI idempotent by transactionId; summary reflects changes after EMI.

C2 (N): UI completion
- Angular Loans page (done): list loans by customer, show schedule, summary, and Apply EMI (dev).
- Acceptance: UI and API in sync; dev-open flows green.

C3 (M): Admin & UX hooks (optional)
- Admin-only “manual re-disbursal” endpoint with idempotency guard (secure profile); UI reachable only to admin role.
- Acceptance: protected endpoint; Postman and UI (admin area) verified under secure profile.

### Track D — Frontend (Angular)

D1 (N): Complete MVP flows
- Cards, Payments, Beneficiaries, Loans, Accounts (done).
- Add toast/alert surface for standardized error messages + correlationId.
- Acceptance: All pages operate against dev-open; errors show readable message and correlationId.

D2 (N): UX/Polish
- Basic form validations; loading states; copy-to-clipboard correlationId; minimal layout polish.
- Acceptance: no “stuck” loading; validation errors user-friendly.

D3 (M): Security integration prep
- Token storage and HttpInterceptor adaptation for bearer tokens (disabled by default).
- Feature flag to switch to secure profile later.

### Track E — Observability

E1 (N): Metrics & Logs consistency
- Enable/verify metrics on all services; consistent logger format (json with correlationId).
- Acceptance: /actuator/prometheus exposes expected metrics; common labels configured.

E2 (M): Dashboards & Tracing
- Provide Grafana JSON: payments/min, cards approvals/min, loan EMIs/day, outstanding loan book.
- Configure OTEL exporters for local tracing (optional) and document setup.

### Track F — Security (Secure Profile)

F1 (M): Identity provider
- Keycloak (or Spring Authorization Server) local config: realm, clients, roles (admin, customer).
- Resource servers: payment-gateway, cards, loans, investments, admin BFF.
- BFF/Gateway: route + RBAC enforcement; forward user context.

F2 (M): Angular wiring
- Login flow; token acquisition; http interceptor adds Authorization header when secure profile is active.

F3 (L): RBAC enforcement & sensitive endpoints
- Admin flows (e.g., card block with reason) enforced by role; customer-facing flows restricted accordingly.
- Acceptance: unauthorized blocked; error shape consistent; Postman includes token flows.

### Track G — CI/CD

G1 (N): Pipelines
- GitHub Actions: Maven build/test with JaCoCo report; Angular lint/build; OWASP dependency-check.
- Gates: coverage thresholds; block PRs with high-severity OWASP findings.

G2 (M): SonarQube (optional)
- Static analysis and maintainability gates; baseline coverage thresholds.

### Track H — Investments

H1 (M): Investments MVP (dev-open)
- investment-onboarding-service: activate investment profile; publish InvestmentProfileActivated (logging).
- product-catalog-service (H2 + cache): seed products; search/filter; mock NAV updates & ProductNAVsUpdated.

H2 (M): OMS and Portfolio
- order-management-service: validate profile; orchestrate debit via payment-gateway; publish InvestmentOrderExecuted.
- portfolio-management-service: consume orders; maintain holdings; revalue on NAV updates.

H3 (L): Kafka profile enablement
- Producers/consumers with idempotency markers; Postman flows.

### Track I — Admin BFF & Reporting

I1 (M): Admin BFF (Spring WebFlux)
- 360 view across customer/accounts/cards/loans/portfolio; admin actions (freeze customer, block card, approve changes).
- Secure profile with RBAC.

I2 (M/L): Reporting
- Read-only endpoints to export CSV/PDF (transactions, portfolio summary).
- Future: CDC to OLAP store (optional).

### Track J — Data Persistence & Migration

J1 (L): Persistence beyond H2
- Targeted services (e.g., card-management, loan-management) migrate to Postgres behind feature flag.
- Flyway scripts; rollback plan.

J2 (L): Backward compatibility
- dev-open remains on H2; migrations feature-flagged; documented runbooks.

### Track K — AI Integrations

K1 (L): Fraud Detection AI
- Replace heuristics in fraud-detection-service with AI decisioning; fallback to rules; expose score + explanation.

K2 (L): KYC/Address & Advisory
- Integrations for OCR/liveness/AML; advisory recommendations and rebalancing suggestions via events.

---

## 5) Sprint Plan (Sequenced, Executable)

Sprint S+1 (Current)
- Cards N2: Kafka E2E finalize; merge main Postman flows; OpenAPI complete.
- Angular: finalize error toasts; minor UX polish; complete Accounts/Loans/Beneficiaries integrations (done).
- Observability baseline: confirm metrics/logs; add Grafana JSON skeletons.
- Deliverable: PRs [cards-n2-kafka], [frontend-mvp], [observability-baseline-1]; All services runnable in dev-open and cards in kafka profile.

Sprint S+2
- CI/CD pipelines (Maven+Angular+OWASP); coverage gates; badge in README.
- Kafka documentation (topics, payload schema versions).
- Angular minor enhancements and docs; developer runbook updates.
- Deliverable: PRs [ci-cd-quality], [kafka-docs], [docs-runbooks].

Sprint S+3
- Security POC: Keycloak, one service as resource server (e.g., card-management) under secure profile; admin endpoint protection.
- Angular token wiring behind a flag; Postman token flows added.
- Deliverable: PRs [secure-profile-poc], [postman-secure].

Sprint S+4
- Investments MVP (onboarding/catalog); events logging; basic Postman flows.
- Admin BFF scaffolding (read 360 view with stubs).
- Deliverable: PRs [investments-i1], [admin-bff-scaffold].

Sprint S+5
- OMS/Portfolio (investments i2) with debit orchestration; portfolio revaluation; tests; Postman flows.
- Observability dashboards iteration.
- Deliverable: PRs [investments-i2], [dashboards-v2].

Sprint S+6+
- Kafka enablement for investments (i3).
- Reporting service phase A (OLTP CSV/PDF).
- Persistence migrations planning & optional execution behind flags.
- Security hardening, SRE alerts/runbooks, tracing enablement (as capacity allows).
- AI integrations scheduled post-production readiness.

---

## 6) Acceptance Criteria & DoD (Per Increment)

- Backward compatible; feature-flagged/profile-gated.
- Unit/integration tests pass; coverage meets gate.
- OpenAPI up to date; event schema docs versioned.
- Postman and/or Angular E2E verified in dev-open (and kafka when applicable).
- Metrics and logs present; correlationId propagated.

---

## 7) Risk Register & Mitigations

- Kafka availability: dev-open uses logging publishers; kafka profile optional; retries/idempotency guard replays.
- Data consistency: idempotency markers and retries; consider outbox later for critical publications.
- Security scope creep: stage secure behind flags; small POC → incremental; dev-open remains for velocity.
- EMI/Amortization complexity: we support partials and interest-first; future arrears handling noted for follow-up.
- Migration risks: feature flags; rollback plans; tested in staging before toggling.

---

## 8) Topics & Payloads (Reference)

- Topics (kafka profile): payment.events, ledger.events, loan.origination.events, loan.management.events, card.issuance.events, card.management.events, investment.events.
- Envelope: { topic, type, timestamp, correlationId, payload }.
- Contract versioning: payloads carry version; document evolution in /docs/events/ with examples.

---

## 9) Runbooks (Short)

- Start dev-open: pwsh -File scripts/start-dev-open.ps1
- Start with kafka for cards: pwsh -File scripts/start-dev-open.ps1 -Kafka
- Angular: cd frontend/customer-angular && npx ng serve --port 5174 --host localhost
- Seed & smoke: pwsh -File scripts/e2e-smoke.ps1 (creates customer, accounts, seed balance, adds/verify beneficiary, initiates transfer).

---

## 10) Documentation & Communication

- README updated with run commands, profiles, ports.
- /docs/ (to be added): OpenAPI snapshots, event schemas, Grafana dashboard JSON, secure profile setup, CI overview, migration runbooks.
- Changelog per sprint with flags toggled and migrations (if any).

---

## 11) Definition of “Seamless and Perfect”

- Every merge maintains green local dev-open flows and quality gates.
- Kafka flows never break dev-open; enablement only increases capability.
- Secure profile introduced incrementally; UI toggles and Postman flows exist for both.
- Idempotency and retries proven by tests and Postman; replays do not duplicate side effects.
- Observability sufficient to debug: correlationId, metrics counters, dashboards.

---

## 12) Ownership & Labels

- Use labels on PRs: [cards], [payments], [lending], [frontend], [kafka], [security], [observability], [ci], [docs], [investments], [admin-bff], [reporting].
- Feature branches per track; small, frequent merges.

---

End of Plan.
