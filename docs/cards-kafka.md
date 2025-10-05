# OmniBank Cards — Kafka E2E Runbook and Event Contracts

Scope:
- Card Issuance publishes CardApplicationApproved on topic card.issuance.events (kafka profile)
- Card Management consumes CardApplicationApproved and auto-creates card (idempotent)
- Query endpoints to verify via Postman:
  - GET /api/v1/cards/issuance/{applicationId}
  - GET /api/v1/cards/customers/{customerId}

Profiles and Topics
- Profiles
  - dev-open (default): logging publisher (no broker), helper endpoints enabled
  - kafka: real Kafka producers/consumers; helper endpoints disabled where event flow exists
- Topics
  - card.issuance.events (outbound from card-issuance; consumed by card-management)
  - payment.events (for payments domain; not directly used by cards but shared envelope)
  - ledger.events (for ledger; not used by cards flow, included for consistent envelope doc)

Standard Event Envelope
All domain events use a common envelope (JSON):

{
  "topic": "<topic-name>",
  "type": "<EventTypeName>",
  "timestamp": "2025-10-05T12:34:56.789Z",
  "correlationId": "postman-cid-or-generated",
  "payload": { ... }  // event-specific body
}

Cards Event: CardApplicationApproved
- Topic: card.issuance.events
- Type: CardApplicationApproved
- Payload schema (v1):

{
  "applicationId": "APP-123",
  "customerId": 12345,
  "productType": "CREDIT_CARD"
}

Idempotency: card-management stores an issuance marker keyed by applicationId to avoid duplicate card creation on replay.

Card Management Consumer (Kafka)
- Listener: KafkaIssuanceConsumer
- On CardApplicationApproved:
  - Calls CardManagementService.createFromIssuanceApproval(applicationId, customerId, productType, correlationId)
  - This internally uses the issuance marker to ensure at-most-once card creation per applicationId
- Profile toggle: active only under SPRING_PROFILES_ACTIVE=kafka and when spring.kafka.* is configured

Run Locally (Kafka)
Prereqs:
- Kafka broker reachable (default: localhost:9092)
- JDK + Maven installed

Start services with script:
- PowerShell 7 (pwsh) recommended:
  pwsh -File scripts/start-dev-open.ps1 -Kafka -KafkaBootstrap "localhost:9092"

What this does:
- Sets SPRING_PROFILES_ACTIVE=kafka and KAFKA_BOOTSTRAP_SERVERS for account-management, payment-gateway, ledger, card-issuance, card-management
- Starts each service in its own terminal window
- dev-open services keep HTTP helpers enabled where applicable; in kafka profile, helper endpoints are disabled where an event exists

Verify with Postman
Use collection: postman/OmniBank.local.postman_collection.json
Use environment: postman/OmniBank.local.environment.json

Sequence:
1) C1 - Card Issuance - Submit Application
   - Sets {{card_app_id}}
2) C2 - Card Issuance - Eligibility Check
3) C3 - Card Issuance - Approve
   - Publishes CardApplicationApproved on card.issuance.events
4) C2K - Card Mgmt (Kafka) - Get Card by Issuance Application
   - Returns auto-created card; sets {{card_id}} if present
5) C2K2 - Card Mgmt (Kafka) - List Cards by Customer
   - Lists cards for the customer; sets {{card_id}} if any

Acceptance:
- Duplicate approvals or replays must not create additional cards (idempotency by applicationId)
- Query endpoints return the auto-created card consistently

Troubleshooting
- Ensure card-issuance and card-management are running with SPRING_PROFILES_ACTIVE=kafka (start script does this when passing -Kafka)
- Check Kafka bootstrap reachability via KAFKA_BOOTSTRAP_SERVERS
- Logs include cid=<correlationId> MDC key for tracing across services
- If using non-default broker address/port, pass -KafkaBootstrap accordingly

Versioning
- Envelope is stable; payloads are versioned by type and implicitly by fields
- Any breaking changes to payload structure should bump EventType to a suffixed variant (e.g., CardApplicationApprovedV2) and maintain compatibility until consumers are migrated
