**OmniBank: Enterprise Banking Platform - Software Design Document**

**Document Version:** 1.0 (Initial Release Blueprint)  
**Date:** September 24, 2025  
**Status:**

**Table of Contents**

- **Introduction & Vision**
  - 1.1 Project Overview
  - 1.2 Business Goals
  - 1.3 Technical Vision
- **Guiding Architectural Principles**
- **System Architecture Overview**
  - 3.1 High-Level Architecture Diagram
  - 3.2 Component Interaction Flow
- **Technology Stack & Rationale**
- **Detailed Service Domain Architecture**
  - 5.1 Domain: Core Banking & Onboarding
  - 5.2 Domain: Payments & Transfers
  - 5.3 Domain: Lending & Credit Services
  - 5.4 Domain: Card Management
  - 5.5 Domain: Investments & Wealth Management
- **Cross-Cutting Concerns (Horizontal Capabilities)**
  - 6.1 Security & Identity Management Architecture
  - 6.2 Observability & Monitoring Architecture
  - 6.3 API Gateway & Edge Service Strategy
- **Data Management & Persistence Strategy**
  - 7.1 Polyglot Persistence Model
  - 7.2 Oracle DB Schema & Strategy
  - 7.3 MongoDB Schema & Strategy
  - 7.4 Redis Caching Strategy
  - 7.5 Reporting & Analytics Data Pipeline
- **Deployment & CI/CD Strategy**
- **Implementation Roadmap & Phasing**
- **Document Information**

**1.0 Introduction & Vision**

**1.1 Project Overview**

This document outlines the complete technical design for the **OmniBank** platform, a greenfield project to build a modern, full-service digital banking ecosystem. The platform will serve both Retail and Corporate customer segments with an exhaustive suite of financial products.

**1.2 Business Goals**

- **Deliver a Superior Customer Experience:** Provide a seamless, intuitive, and feature-rich digital interface for all banking needs.
- **Achieve Operational Excellence:** Automate core processes, from onboarding to loan origination, to reduce manual effort and operational costs.
- **Enhance Security & Compliance:** Build a secure and compliant platform that earns customer trust and meets regulatory requirements.
- **Enable Business Agility:** Create an extensible architecture that allows for the rapid development and deployment of new financial products.

**1.3 Technical Vision**

The platform will be a cloud-native, distributed system built on a microservices architecture. It will be event-driven, API-first, and designed for high availability, scalability, and resilience. The core tenets are security, observability, and automation.

**2.0 Guiding Architectural Principles**

- **Domain-Driven Design (DDD):** Services are modeled around business domains, creating a clear and bounded context for each, reducing cognitive load and improving maintainability.
- **Separation of Concerns:** Each microservice has a single, well-defined responsibility. For example, the ledger-service only handles accounting; it knows nothing of fraud detection.
- **Loose Coupling & High Cohesion:** Services are highly cohesive (focused on their domain) and loosely coupled (communicating via well-defined APIs and asynchronous events), allowing them to be developed, deployed, and scaled independently.
- **Centralized Security, Decentralized Execution:** Security policy is defined centrally (in Keycloak and the API Gateway), but enforcement is decentralized, ensuring a consistent security posture across the entire platform.
- **Immutable Infrastructure:** All deployments will be automated using Ansible. Servers will be treated as immutable; changes are made by deploying new, updated instances, not by modifying existing ones.

**3.0 System Architecture Overview**

**3.1 High-Level Architecture Diagram**

+----------------+ +---------------------+ +------------------------+ +--------------------+

| Angular Apps | | Spring Cloud Gateway| | Business Microservices | | Data Stores |

| (Retail/Admin) |----->| (Security, Routing) |----->| (Java/Spring Boot) |----->| (Oracle, Mongo, Redis)|

+----------------+ +----------+----------+ +-----------+------------+ +--------------------+

^ | |

| | |

+-------------------+ | | +------------+------------+

| External Systems |---------+ +--------->| Cross-Cutting Services |

| (Card Networks, | (Auth, Reporting, etc.)|

| Credit Bureaus) | +-----------------------+

+-------------------+

|

|

+--------v--------+

| Apache Kafka |

| (Event Backbone)|

+--------^--------+

|

(Events are published and subscribed to by all microservices)

**3.2 Component Interaction Flow (Example: Fund Transfer)**

- **Client:** User initiates a transfer from the Angular App.
- **API Gateway:** Receives the request, validates the user's JWT from Keycloak, and routes the request to payment-gateway-service.
- **Orchestrator (payment-gateway-service):** Performs business validation (checks limits, beneficiary status, balance).
- **Event Publication:** The service publishes a PaymentInitiated event to a Kafka topic.
- **Core Processor (ledger-service):** Subscribes to the Kafka topic, consumes the event, and performs the atomic debit/credit operations in its Oracle DB transaction.
- **Event Publication:** ledger-service publishes a TransactionPosted event.
- **Downstream Consumers:**
  - account-management-service consumes the event to update its own balance record.
  - notification-service consumes the event to send an SMS/email to the user.

**4.0 Technology Stack & Rationale**

| **Category** | **Tools & Technologies** | **Detailed Rationale & Usage** |
| --- | --- | --- |
| **Frontend** | Angular 17+, TypeScript, NGRX | **Angular:** Provides a mature, structured framework for building large-scale, maintainable SPAs. **NGRX:** Implements Redux patterns for predictable and robust state management, essential for a complex application. |
| **Backend** | Java 17+, Spring Boot 3+, Spring Cloud | **Spring Boot:** Accelerates development with convention-over-configuration, embedded servers, and production-ready features. **Spring Cloud:** Provides a suite of tools for distributed systems (e.g., Gateway, config management). |
| **RDBMS** | **Oracle Database 19c (or later)** | **Rationale:** Chosen for its proven enterprise-grade features, including robust ACID compliance, advanced security options (TDE, VPD), high-availability (RAC), and comprehensive support. It is the gold standard for core transactional systems. |
| **Document DB** | MongoDB | **Rationale:** Its flexible schema is ideal for managing the semi-structured, evolving data of workflows like customer onboarding and loan origination, which are essentially "case files." |
| **Cache** | Redis | **Rationale:** Provides sub-millisecond latency for caching frequently accessed, slow-changing data (e.g., card limits, session data), drastically reducing database load and improving response times. |
| **Security** | Keycloak, Spring Security, JWT | **Keycloak:** A dedicated, open-source IAM that externalizes user management, authentication (MFA), and authorization (OAuth 2.0/OIDC), decoupling security logic from business logic. |
| **API Gateway** | Spring Cloud Gateway | **Rationale:** A reactive, non-blocking gateway that serves as the single entry point. It will handle SSL termination, request routing, JWT validation, and rate limiting, acting as a security shield for the microservices. |
| **Messaging** | Apache Kafka | **Rationale:** The central nervous system of the platform. Its durable, partitioned logs enable reliable event-driven communication, event sourcing, and real-time data streaming to analytical systems. |
| **Observability** | ELK Stack, Prometheus, Grafana, Jaeger | **Rationale:** A best-in-class open-source stack providing end-to-end visibility. This is non-negotiable for diagnosing issues and understanding performance in a distributed environment. |
| **Build & CI/CD** | Maven, Jenkins, Ansible, SonarQube | **Rationale:** A fully automated pipeline. **Maven:** Dependency management. **Jenkins:** CI orchestration. **SonarQube:** Static code analysis for quality and security. **Ansible:** Infrastructure-as-code for consistent deployments. |

**5.0 Detailed Service Domain Architecture**

**5.1 Domain: Core Banking & Onboarding**

**Domain Purpose:** To manage the customer's identity, profile, and their fundamental deposit accounts. This domain is the foundational layer of the customer relationship and the primary target for AI-driven onboarding automation.

- **Purpose:** To manage the secure, compliant, and highly automated workflow for new customer applications. This service is designed as a state machine orchestrated by both user actions and AI agent interventions.
- **Core Responsibilities:**
  - **State Management:** Manage the detailed state of a customer application (e.g., STARTED, DOCUMENTS_UPLOADED, AWAITING_VERIFICATION, FLAGGED_FOR_MANUAL_REVIEW, VERIFIED, APPROVED).
  - **Data Ingestion:** Handle the collection of initial personal data and the secure upload of identity documents (e.g., Passport, Driver's License).
  - **AI-Driven Verification (Primary Path):**
    - Upon document submission, publish a VerificationRequired event.
    - An **Automated KYC Agent** subscribes to this event.
    - The agent invokes third-party services for Optical Character Recognition (OCR) to extract text, facial recognition to match the photo with a selfie, and liveness detection.
    - The agent performs automated checks against internal and external watchlists (AML checks).
    - Based on a confidence score, the agent makes a decision.
  - **Action Execution:** The KYC Agent calls a secure API endpoint on this service to move the application forward.
  - **Exception Handling (Manual Path):** If the AI agent's confidence score is low, or if rules dictate (e.g., Politically Exposed Person), the agent will transition the application state to FLAGGED_FOR_MANUAL_REVIEW and publish an event, which creates a task for a human officer in the admin portal.
- **Key API Endpoints:**
  - POST /api/v1/onboarding/start: (User-facing) Initiates a new application.
  - POST /api/v1/onboarding/{appId}/documents: (User-facing) Uploads ID documents.
  - POST /api/v1/onboarding/agent/update-status: **(AI Agent/Admin-facing)** A secure, privileged endpoint for the KYC agent or a human admin to update the application's status (e.g., to VERIFIED or REJECTED) and provide justification.
- **Data Model (MongoDB):**
  - **Collection:** onboarding_applications
  - **Document Structure:**

{

"applicationId": "...",

"state": "AWAITING_VERIFICATION",

"documents": \[{"type": "PASSPORT", "url": "...", "status": "UPLOADED"}\],

"verificationHistory": \[

{

"agent": "AutomatedKYCAgentV2",

"timestamp": "...",

"action": "OCR_EXTRACTION",

"outcome": "SUCCESS",

"confidenceScore": 0.98,

"details": {"extractedName": "John Doe"}

},

{

"agent": "AutomatedKYCAgentV2",

"timestamp": "...",

"action": "AML_CHECK",

"outcome": "PASS"

}

\]

}

- **Event Interactions:**
  - **Publishes:**
    - ApplicationStarted: When a new application is created.
    - DocumentsUploaded: Signals that documents are ready for processing.
    - VerificationRequired: The specific trigger for the KYC Agent.
    - OnboardingApproved: The final success event, containing all verified customer data.
    - ManualReviewRequired: The trigger for the human workflow.
  - All events are published to the onboarding.events Kafka topic.
- **Non-Functional Requirements (NFRs):**
  - **Auditability:** Every state change and every action taken by the AI agent must be immutably logged.
  - **Extensibility:** The system must allow for new verification steps or different AI agents to be introduced into the workflow without code changes.
- **Purpose:** To be the canonical, authoritative source of truth for all customer master data. It serves as the secure repository for data verified by the onboarding process and subsequent self-service updates.
- **Core Responsibilities:**
  - **Master Record Management:** Create and maintain the Customer Information File (CIF) upon successful onboarding.
  - **Self-Service Automation:** Manage the workflow for self-service requests (e.g., address change, mobile number update).
  - **AI-Agent Integration for Updates:**
    - A user submits an address change request with a new utility bill as proof.
    - The service marks the request as PENDING_VERIFICATION and publishes an AddressUpdateVerificationRequired event.
    - An **Address Verification Agent** consumes this event.
    - The agent performs OCR on the utility bill, validates the address using external services (e.g., postal service APIs), and checks for any fraud indicators.
    - If confident, the agent calls a privileged API on this service to approve the request, which then updates the master record. If not, it flags the request for manual review.
- **Key API Endpoints:**
  - GET /api/v1/customers/{customerId}: (User/Service-facing) Fetches the customer's profile.
  - POST /api/v1/customers/{customerId}/address-update-requests: (User-facing) Initiates a request to update the address.
  - POST /api/v1/customers/agent/approve-update-request: **(AI Agent/Admin-facing)** A secure endpoint to approve a pending self-service request.
- **Data Model (Oracle DB):**
  - **Schema:** CUSTOMER_MASTER
  - **Key Tables:**
    - CUSTOMERS (CUSTOMER_ID NUMBER PK, CIF_ID VARCHAR2(20) UNIQUE, STATUS VARCHAR2(15))
    - ADDRESSES (ADDRESS_ID NUMBER PK, CUSTOMER_ID FK, ADDRESS_LINE1, CITY, STATUS VARCHAR2(15) DEFAULT 'ACTIVE') - _Note: Includes a status to support historical addresses._
    - UPDATE_REQUESTS (REQUEST_ID NUMBER PK, CUSTOMER_ID FK, REQUEST_TYPE VARCHAR2(30), STATUS VARCHAR2(20), SUBMITTED_DATA CLOB, APPROVED_BY VARCHAR2(100), APPROVED_TS TIMESTAMP)
- **Event Interactions:**
  - **Subscribes to:** OnboardingApproved (from onboarding.events) to trigger the creation of the permanent customer profile.
  - **Publishes to:** customer.profile.events topic with events like CustomerProfileCreated, CustomerAddressUpdated, ContactInfoUpdated.
- **NFRs:**
  - **Data Integrity:** Strict ACID compliance for all master data.
  - **Auditability:** All changes to customer data, whether by a user, admin, or AI agent, must be logged in an audit table.
- **Purpose:** To manage the lifecycle of all customer deposit accounts. This service is less about direct AI intervention and more about providing clean, reliable data for other services and agents to use.
- **Core Responsibilities:**
  - **Account Lifecycle:** Handle the creation, activation, dormancy, and closure of Savings Accounts, Current Accounts, Fixed Deposits (FDs), and Recurring Deposits (RDs).
  - **Business Logic:** Manage all business logic related to these accounts, such as interest calculation, maturity processing for FDs/RDs, and balance maintenance.
  - **Data Provision:** Serve as the authoritative source for account status and balance, providing this data to other services (like payment-gateway-service) when they need to make decisions.
  - **Automated Account Opening:** Can be configured to automatically open a default savings account upon receiving a CustomerProfileCreated event, further streamlining the onboarding process.
- **Key API Endpoints:**
  - POST /api/v1/accounts: Creates a new account for a customerId.
  - GET /api/v1/customers/{customerId}/accounts: Lists all accounts for a customer.
  - GET /api/v1/accounts/{accountNumber}/balance: Retrieves the current, real-time balance.
- **Data Model (Oracle DB):**
  - **Schema:** ACCOUNT_MASTER
  - **Key Tables:**
    - ACCOUNTS (ACCOUNT_NUMBER VARCHAR2(30) PK, CUSTOMER_ID FK, ACCOUNT_TYPE, STATUS, BALANCE NUMBER(19,4), OPENING_DATE DATE)
    - DEPOSIT_PRODUCTS (DEPOSIT_ID NUMBER PK, ACCOUNT_NUMBER FK, PRINCIPAL, INTEREST_RATE, MATURITY_DATE)
- **Event Interactions:**
  - **Subscribes to:** CustomerProfileCreated (from customer.profile.events) as a trigger for automated account creation.
  - **Publishes to:** account.events topic with events like AccountCreated, AccountStatusChanged, and the critical BalanceUpdated event.
- **NFRs:**
  - **Transactional Integrity (ACID):** Non-negotiable for all financial data.
  - **High Performance:** Balance check APIs must have very low latency (p99 < 50ms) as they are on the critical path for many other operations.

**5.2 Domain: Payments & Transfers**

**5.2.1 Domain Overview**

This domain is the cardiovascular system of the bank, responsible for the secure, accurate, and reliable movement of all funds. The architectural priorities are **security, atomicity, auditability, and low latency**. Every financial transaction must be processed as an indivisible unit, leaving a permanent, immutable record.

This domain is a primary area for real-time AI intervention. A **Fraud Detection AI Agent** will be integrated directly into the transaction processing pipeline to analyze and score transactions for risk _before_ they are executed, providing a dynamic and intelligent layer of security.

**5.2.2 Microservice: payment-gateway-service**

To serve as the single, secure entry point and orchestration engine for all payment initiation requests. This service acts as a "smart gatekeeper," performing all necessary business rule validations and risk assessments before a transaction is handed off for final processing.

- **Request Ingestion & Validation:** Receive all payment requests (Internal Transfer, External Transfer, Bill Payment) and perform initial syntactic and semantic validation.
- **Pre-Execution Checks:** Orchestrate a series of synchronous checks before committing a payment:
  - Query customer-profile-service to check the customer's status and transfer limits.
  - Query account-management-service to verify account status and sufficient funds.
  - Query beneficiary-management-service to ensure the payee is valid and active.
- **AI-Driven Fraud Analysis (Critical Path):**
  - After passing initial checks, the service constructs a detailed transaction context object (containing user details, transaction amount, beneficiary info, user's device fingerprint, time of day, etc.).
  - It then makes a **synchronous, blocking API call** to a dedicated fraud-detection-service.
  - The service awaits a response, which includes a risk score (e.g., 0.01 to 1.0) and a recommended action (ALLOW, CHALLENGE, BLOCK).
- **Decision Orchestration:**
  - If the recommendation is ALLOW, it proceeds to publish the payment event for processing.
  - If CHALLENGE, it returns a specific response to the frontend, prompting the user for an additional authentication factor (e.g., a different OTP or a security question).
  - If BLOCK, it immediately rejects the transaction and publishes a high-priority FraudulentTransactionBlocked event.
- **Status Tracking:** Maintain the state of the payment orchestration itself for audit and debugging purposes.

| **Method** | **Path** | **Description** | **Sample Request Body / Response Body** |
| --- | --- | --- | --- |
| **POST** | /api/v1/payments/internal-transfer | Initiates a transfer between two OmniBank accounts. | **Request:** { "fromAccount": "...", "toAccount": "...", "amount": 100.00, "currency": "USD" } &lt;br&gt; **Response (Success):** { "paymentId": "...", "status": "PROCESSING" } &lt;br&gt; **Response (Challenge):** { "paymentId": "...", "status": "MFA_CHALLENGE_REQUIRED" } |
| **POST** | /api/v1/payments/external-transfer | Initiates an inter-bank transfer (e.g., ACH/SWIFT). |     |
| **GET** | /api/v1/payments/{paymentId}/status | Checks the current status of a payment initiation. | **Response:** { "status": "COMPLETED" } |

- **Schema:** PAYMENT_MASTER
- **Rationale:** To provide a transactional audit trail of every payment attempt and its outcome, including the fraud score received.
- **Key Tables:**
  - PAYMENT_REQUESTS (PAYMENT_ID NUMBER PK, PAYMENT_UUID VARCHAR2(36) UNIQUE, CUSTOMER_ID FK, STATUS VARCHAR2(20), FRAUD_SCORE NUMBER(5,4), FRAUD_ACTION_TAKEN VARCHAR2(15), REQUEST_TS TIMESTAMP)
- **Publishes to Kafka Topic payment.events:**
  - **Event:** PaymentApprovedForProcessing (if fraud check passes)
    - **Payload:** Contains all necessary details for the ledger-service to execute the transaction.
  - **Event:** FraudulentTransactionBlocked
    - **Payload:** Rich event containing all transaction details and fraud score for alerting and analysis.

**5.2.3 Microservice: ledger-service**

To be the ultimate, immutable source of truth for all financial movements. This service is intentionally simple and "dumb" about business rules. Its sole responsibility is to accurately record debits and credits in a transactionally-safe manner, ensuring the bank's books are always balanced.

- **Atomic Transaction Processing:** Expose a single, critical endpoint that accepts a transaction object. It must perform all debit and credit operations within a single, atomic database transaction.
- **Double-Entry Bookkeeping:** Enforce the principle that for every transaction, the sum of debits must equal the sum of credits.
- **Immutability:** Transactions are never deleted or updated. Errors are corrected by posting a new, reversing transaction. This provides a complete and unalterable audit trail.
- **Transaction History:** Provide an endpoint to query the detailed transaction history for any given account.

| **Method** | **Path** | **Description** | **Sample Request Body / Response Body** |
| --- | --- | --- | --- |
| **POST** | /api/v1/internal/ledger/transactions | **(Internal Endpoint)** Executes a financial transaction. Called by other services, not exposed externally. | **Request:** { "entries": \[ {"account": "...", "amount": -100.00}, {"account": "...", "amount": 100.00} \] } &lt;br&gt; **Response:** { "transactionId": "...", "status": "POSTED" } |
| **GET** | /api/v1/accounts/{accountNumber}/history | Retrieves the transaction history for a specific account. |     |

- **Schema:** LEDGER
- **Rationale:** This is the most critical transactional system in the bank. Oracle's ACID guarantees, reliability, and performance are essential.
- **Key Tables:**
  - TRANSACTIONS (TRANSACTION_ID NUMBER PK, TRANSACTION_UUID VARCHAR2(36) UNIQUE, TRANSACTION_TYPE VARCHAR2(20), STATUS VARCHAR2(15), POSTED_TS TIMESTAMP)
  - TRANSACTION_ENTRIES (ENTRY_ID NUMBER PK, TRANSACTION_ID FK, ACCOUNT_NUMBER VARCHAR2(30), AMOUNT NUMBER(19,4), DIRECTION CHAR(1)) -- _Direction is 'D' for Debit, 'C' for Credit._
- **Subscribes to Kafka Topic payment.events:**
  - **Event:** PaymentApprovedForProcessing. This is the primary trigger for this service to perform its core function.
- **Publishes to Kafka Topic ledger.events:**
  - **Event:** TransactionPosted. This is a vital, high-volume event.
    - **Payload:** { "transactionId": "...", "entries": \[...\], "timestamp": "..." }. It signals to the rest of the system that money has officially moved.

**5.2.4 Microservice: beneficiary-management-service**

To securely manage the lifecycle of customer payees (beneficiaries). Adding a new beneficiary is a high-risk operation, and this service encapsulates the logic to make it safe.

- Handle CRUD operations for internal and external beneficiaries.
- Implement security workflows for adding new payees, including mandatory OTP verification.
- Enforce bank-defined rules, such as a "cooling-off" period (e.g., 24 hours) after adding a new beneficiary, during which transfer limits are significantly lower. This logic will be managed by this service.
- **Beneficiary Risk Scoring:** While not a real-time process, an offline AI agent can periodically analyze beneficiary data. When a user adds a new beneficiary, this service can publish a BeneficiaryAdded event. An AI agent can consume this, analyze the beneficiary's details (e.g., is the account from a high-risk country?), and associate a risk score with the beneficiary in this service's database via an internal API. This score can then be used by the fraud-detection-service as an additional feature during transaction analysis.
- **Schema:** CUSTOMER_MASTER
- **Key Tables:**
  - BENEFICIARIES (BENEFICIARY_ID NUMBER PK, OWNING_CUSTOMER_ID FK, NICKNAME VARCHAR2(100), ACCOUNT_NUMBER, BANK_CODE, STATUS VARCHAR2(15), ADDED_TS TIMESTAMP, RISK_SCORE NUMBER(5,4))

**5.2.5 Microservice: payment-network-integrator-service**

To act as an "anti-corruption layer," isolating the internal banking system from the specific, often legacy, protocols of external payment networks like SWIFT, ACH, or Fedwire.

- Translate the canonical internal payment format into the specific message formats required by external networks (e.g., SWIFT MT103, ISO 20022 XML).
- Handle the secure, stateful communication with these external gateways.
- Process acknowledgments (ACK/NACK) and settlement files from these networks and translate them back into internal Kafka events.

**5.3 Domain: Lending & Credit Services**

**5.3.1 Domain Overview**

This domain manages the entire lifecycle of credit products, from the initial customer application to the final repayment. It is characterized by two distinct phases: **Origination** (a complex, document-heavy workflow focused on risk assessment) and **Servicing** (the long-term, transaction-oriented management of an active loan).

Our architecture will reflect this separation and deeply integrate an **AI Underwriting Agent** into the origination process to automate credit scoring, policy checks, and final decision-making for a significant portion of applications.

**5.3.2 Microservice: loan-origination-service**

To act as the master workflow engine for the entire loan application process. It orchestrates the collection of data, the execution of underwriting rules, the decisions from the AI Agent, and the final disbursal of funds.

- **Application & Document Management:** Manage the state of a loan application (e.g., STARTED, DOCUMENTS_COLLECTED, UNDERWRITING_IN_PROGRESS, APPROVED, REJECTED) and handle the secure upload of required documents (payslips, bank statements, etc.).
- **Orchestration of Underwriting:**
  - Upon submission of a complete application, it publishes a LoanApplicationSubmitted event.
  - This event triggers the **AI Underwriting Agent**.
  - The agent then orchestrates the entire risk assessment process by calling other specialized services.
- **Decision Finalization:** The service provides a secure endpoint for the AI Underwriting Agent (or a human underwriter) to post the final decision.
- **Fund Disbursal:** If the decision is APPROVED, this service is responsible for initiating the fund transfer by calling the payment-gateway-service.

| **Method** | **Path** | **Description** | **Sample Request Body / Response Body** |
| --- | --- | --- | --- |
| **POST** | /api/v1/loans/applications | (User-facing) Submits a new loan application. | **Request:** { "loanType": "PERSONAL", "amount": 10000, ... } &lt;br&gt; **Response:** { "applicationId": "...", "status": "DOCUMENTS_REQUIRED" } |
| **POST** | /api/v1/loans/applications/{appId}/documents | (User-facing) Uploads supporting documents. |     |
| **POST** | /api/v1/loans/agent/record-decision | **(AI Agent/Admin-facing)** Records the final underwriting decision for an application. | **Request:** { "applicationId": "...", "decision": "APPROVED", "approvedAmount": 10000, "interestRate": 8.5, "decisionBy": "AI_Underwriter_V3", "justification": "..." } &lt;br&gt; **Response:** 200 OK |

- **Collection:** loan_applications
- **Rationale:** A loan application is a "case file" that collects a variety of structured and unstructured data over its lifecycle. A document database is perfectly suited for this evolving, semi-structured data model.
- **Document Structure:**

{

"applicationId": "...",

"status": "APPROVED",

"loanDetails": { "type": "PERSONAL", "requestedAmount": 10000 },

"applicantData": { ... },

"documents": \[ ... \],

"underwritingHistory": \[

{ "stage": "CREDIT_SCORE_PULLED", "agent": "AI_Underwriter_V3", "timestamp": "...", "outcome": "SUCCESS", "details": {"score": 780} },

{ "stage": "POLICY_CHECK", "agent": "AI_Underwriter_V3", "timestamp": "...", "outcome": "PASS" }

\],

"finalDecision": {

"decision": "APPROVED",

"approvedAmount": 10000,

"interestRate": 8.5,

"decisionBy": "AI_Underwriter_V3"

}

}

- **Publishes to Kafka Topic loan.origination.events:**
  - **Event:** LoanApplicationSubmitted (The trigger for the AI Underwriting Agent).
  - **Event:** LoanApproved (The critical trigger for the loan-management-service).
  - **Event:** LoanDisbursed.
  - **Event:** ManualUnderwritingRequired.

**5.3.3 Microservice: credit-scoring-service**

To provide a dedicated, high-performance service for assessing the creditworthiness of an applicant. This service is a core tool used by the **AI Underwriting Agent**.

- **Data Aggregation:** Expose a single endpoint that, when called, gathers data from multiple sources:
  - Internal: Customer's account history, existing loan performance.
  - External: Securely integrate with third-party credit bureaus (e.g., Experian, TransUnion) via their APIs.
- **AI/ML Model Execution:** Host and execute a machine learning model (e.g., a gradient-boosted tree or a neural network) trained on historical loan data to predict the probability of default.
- **Score & Explainability:** Return a comprehensive credit report including a final score (e.g., 300-850), the key factors that influenced the score (explainable AI), and any red flags.

| **Method** | **Path** | **Description** | **Sample Request Body / Response Body** |
| --- | --- | --- | --- |
| **POST** | /api/v1/internal/scoring/calculate | **(Internal Endpoint)** Called by the AI Underwriting Agent to get a credit assessment. | **Request:** { "applicantId": "..." } &lt;br&gt; **Response:** { "creditScore": 780, "probabilityOfDefault": 0.05, "keyFactors": \["Low credit card utilization", "Long credit history"\], "bureauData": {...} } |

**5.3.4 Microservice: loan-management-service**

To be the master system of record for all active, disbursed loans. Once a loan is approved and the funds are sent, this service takes over and manages the entire servicing lifecycle until the loan is paid off.

- **Loan Account Creation:** Listen for the LoanApproved event and create the official loan account in its database.
- **Amortization & Scheduling:** Generate the complete repayment schedule (EMI schedule) based on the approved loan amount, interest rate, and tenure.
- **Financial Operations:** Handle all core financial logic, including daily interest accrual and tracking of EMI payments. It listens for TransactionPosted events from the ledger-service to identify and apply repayments.
- **Lifecycle Management:** Manage the loan's status (ACTIVE, PAID_OFF, IN_ARREARS, DEFAULTED).
- **Servicing:** Provide APIs for users to view their loan details, payment history, and generate statements.
- **Collections Trigger:** If a loan enters a state of default, it publishes a LoanDefaulted event, which can trigger a collections workflow (handled by another service or an AI agent).

| **Method** | **Path** | **Description** | **Sample Request Body / Response Body** |
| --- | --- | --- | --- |
| **GET** | /api/v1/loans/customers/{customerId} | (User/Admin-facing) Lists all active loans for a customer. |     |
| **GET** | /api/v1/loans/{loanAccountNumber} | (User/Admin-facing) Gets the full details and repayment schedule for a specific loan. |     |

- **Schema:** LOAN_MASTER
- **Rationale:** Loan account data is highly structured and requires absolute transactional precision for financial calculations. An ACID-compliant RDBMS is mandatory.
- **Key Tables:**
  - LOAN_ACCOUNTS (LOAN_ACCOUNT_ID NUMBER PK, LOAN_ACCOUNT_NUMBER VARCHAR2(30) UNIQUE, CUSTOMER_ID FK, STATUS VARCHAR2(20), PRINCIPAL_DISBURSED NUMBER(19,4), CURRENT_BALANCE NUMBER(19,4))
  - LOAN_SCHEDULE (SCHEDULE_ID NUMBER PK, LOAN_ACCOUNT_ID FK, EMI_DATE DATE, EMI_AMOUNT NUMBER(19,4), PRINCIPAL_COMPONENT, INTEREST_COMPONENT, PAYMENT_STATUS VARCHAR2(20))
- **Subscribes to:**
  - LoanApproved (from loan.origination.events): The trigger to create the loan account.
  - TransactionPosted (from ledger.events): To identify and apply EMI payments.
- **Publishes to Kafka Topic loan.management.events:**
  - LoanAccountCreated, EmiPaid, EmiMissed, LoanDefaulted, LoanClosed.

**5.3.5 Microservice: collateral-management-service**

To manage the lifecycle of assets pledged as security for secured loans (e.g., Home Loans, Auto Loans).

- CRUD operations for collateral assets (property address, vehicle registration info).
- Store valuation reports and insurance details related to the collateral.
- Manage the legal lien on the collateral.
- Handle the automated process of releasing the lien once the LoanClosed event is received for the associated loan.

**5.4 Domain: Card Management**

**5.4.1 Domain Overview**

This domain governs the entire physical and digital lifecycle of debit and credit cards. It is one of the most customer-interactive parts of the bank. The architecture must support both high-volume, real-time transaction processing and a rich set of user-driven self-service features.

The core of this domain's AI integration is a **Real-time Fraud Detection Agent** that analyzes every single card authorization request in milliseconds. Additionally, AI agents will be used to personalize customer experiences and automate dispute resolution.

**5.4.2 Microservice: card-issuance-service**

To manage the application and fulfillment workflow for new debit and credit cards. This service orchestrates the process from the initial request to the final dispatch of the physical card.

- **Application Workflow:** Manage the state of a card application (SUBMITTED, ELIGIBILITY_CHECKED, APPROVED, SENT_TO_VENDOR, DISPATCHED).
- **Eligibility Checks:**
  - For debit cards, it verifies the existence of an active and eligible bank account by querying the account-management-service.
  - For credit cards, it initiates a credit eligibility check by publishing an event that the **AI Underwriting Agent** (from Domain 3) can pick up, ensuring a consistent credit decisioning process across all products.
- **Vendor Integration:** Integrate with external card personalization and printing vendors via secure APIs to submit approved card details for manufacturing and dispatch.
- **Virtual Card Generation:** For certain products, it will have the capability to instantly generate virtual card details upon approval for immediate use by the customer.
- **Publishes to card.issuance.events:**
  - **Event:** CreditCardApplicationSubmitted (Trigger for the AI Underwriting Agent).
  - **Event:** CardApplicationApproved (The trigger for the card-management-service to create the permanent card record).

**5.4.3 Microservice: card-management-service**

To be the **single source of truth** for all issued card data and to empower customers with real-time, self-service control over their cards.

- **Card Master Record:** Creates and maintains the permanent record for every card (debit and credit) upon receiving a CardApplicationApproved event.
- **Lifecycle Management:** Manages the card's primary status (AWAITING_ACTIVATION, ACTIVE, BLOCKED_BY_USER, BLOCKED_BY_BANK, EXPIRED).
- **User Control APIs:** Provides a comprehensive suite of secure APIs for the frontend, allowing customers to:
  - Activate a new card.
  - View card details securely (e.g., revealing the full card number/CVV of a virtual card after MFA).
  - Instantly block and unblock their card.
  - Set and modify granular transaction limits (e.g., separate limits for ATM withdrawal, Point-of-Sale, and e-commerce).
  - Enable/disable specific transaction channels (e.g., international transactions, contactless payments).
  - Request a new PIN or a replacement card.
- **Schema:** CARD_MASTER
- **Rationale:** Card data is highly structured, security-critical, and requires strong consistency.
- **Key Tables:**
  - CARDS (CARD_ID NUMBER PK, CARD_NUMBER_HASH VARCHAR2(256), LAST_4_DIGITS VARCHAR2(4), CUSTOMER_ID FK, STATUS VARCHAR2(20))
  - CARD_LIMITS (LIMIT_ID NUMBER PK, CARD_ID FK, CHANNEL_TYPE VARCHAR2(20), LIMIT_AMOUNT NUMBER(19,4), IS_ACTIVE CHAR(1))
- **Subscribes to:** CardApplicationApproved (from card.issuance.events).
- **Publishes to card.management.events:**
  - **Event:** CardCreated, CardStatusUpdated, CardLimitsChanged. These events are published with very low latency to ensure that any downstream services (especially the card-authorization-service) have the most up-to-date information.

**5.4.4 Microservice: card-authorization-service**

To provide an ultra-fast, highly available, real-time decision engine for all card-based transaction authorizations. **This is a mission-critical, low-latency service.**

- **Real-time Decisioning:** Expose a secure internal endpoint that receives authorization requests (typically in ISO 8583 format, translated by a network gateway). It must return an APPROVE or DECLINE decision within a very strict SLA (e.g., < 50 milliseconds).
- **High-Speed Checks:** Perform a sequence of rapid checks using locally cached data for speed:
  - **Card Status & Limits:** Verify the card's status and check against all user-defined and bank-defined limits.
  - **Funds Check:** Verify the available balance (for debit) or credit limit (for credit).
- **Real-time AI Fraud Analysis (Critical Path):**
  - This is the final and most important check. It constructs a feature vector from the transaction data (amount, merchant category, country, time of day, etc.).
  - It makes a synchronous, low-latency gRPC call to a dedicated fraud-detection-service.
  - The **Fraud Detection AI Agent**'s model scores the transaction in real-time.
  - If the score is below the threshold, the service returns APPROVE. If it's above, it returns DECLINE.
- **Logging & Eventing:** Log every single authorization attempt and its outcome. If approved, publish an event for financial settlement.
- **Primary Store:** **Redis**. This service will maintain a local cache of all active card statuses, limits, and balances. It keeps this cache fresh by subscribing to events from card-management-service and account-management-service. This avoids slow database calls during the critical authorization path.
- **Secondary Store:** Logs authorization attempts asynchronously to a high-throughput database (Oracle or NoSQL) for audit.
- **Subscribes to (to update its Redis cache):** CardStatusUpdated, CardLimitsChanged, BalanceUpdated.
- **Publishes to card.transactions.events:**
  - **Event:** CardTransactionAuthorized (The trigger for the ledger-service and credit-card-billing-service).
  - **Event:** CardTransactionDeclined.

**5.4.5 Microservice: credit-card-billing-service**

To manage the complex financial lifecycle of credit cards, which operates on periodic billing cycles rather than immediate debits.

- **Billing Cycle Management:** Define and manage billing cycles for each credit card account.
- **Transaction Accumulation:** Subscribe to CardTransactionAuthorized events and accumulate transactions for the current billing cycle.
- **Statement Generation:** At the end of a cycle, run a batch process to generate the official monthly statement, calculating total due, minimum payment, interest on revolving balances, and any applicable fees.
- **Payment Processing:** Process payments made by customers against their credit card bill.
- **Rewards & Loyalty:** Manage the entire rewards and loyalty points system (accrual on spends and redemption).
- **Dispute Resolution Agent:** When a customer disputes a transaction, a TransactionDisputeRaised event is published. A **Dispute Resolution AI Agent** can consume this event and perform initial investigations:
  - Analyze the transaction details and compare them to the customer's typical spending patterns.
  - Automatically initiate a "chargeback" request with the card network for clear-cut fraud cases.
  - Provide the customer with a provisional credit while the investigation is ongoing.
  - Flag complex cases for a human agent.

**5.5 Domain: Investments & Wealth Management**

**5.5.1 Domain Overview**

This domain empowers customers to grow their wealth by providing a platform to discover, purchase, and manage a variety of investment products. The architectural focus is on **data accuracy, reliable integration with external financial systems (like exchanges and registrars), and providing personalized, data-driven insights**.

The core AI component is a **Robo-Advisory Agent**, which acts as a virtual financial advisor, guiding customers toward suitable investments based on their individual financial situation and goals.

**5.5.2 Microservice: investment-onboarding-service**

To manage the distinct regulatory and suitability workflow required before a customer can begin investing. This process is separate from the core bank KYC and is mandatory for compliance.

- **Risk Profiling:** Administer a detailed risk-profiling questionnaire to understand the customer's risk tolerance, investment horizon, and financial goals.
- **Profile Calculation:** Based on the questionnaire's outcome, assign the customer a specific risk profile (e.g., CONSERVATIVE, BALANCED_GROWTH, AGGRESSIVE).
- **Compliance Checks:** Collect any additional information required by securities regulators.
- **Profile Management:** Manage the state of the investment profile (NOT_STARTED, IN_PROGRESS, ACTIVE). An active profile is a prerequisite for any investment activity.
- **Publishes to investment.profile.events:**
  - **Event:** InvestmentProfileActivated (The trigger that unlocks investment capabilities for a customer).

**5.5.3 Microservice: product-catalog-service**

To be the central, reliable source of truth for all investment products offered on the platform and their associated market data.

- **Data Aggregation:** Integrate with external market data vendors and Asset Management Companies (AMCs) to fetch and store detailed information on all available mutual funds, ETFs, stocks, and bonds.
- **Data Repository:** Store key product information: fund name, expense ratio, investment strategy, underlying assets, historical performance data, and ratings.
- **Market Data Updates:** Run a scheduled daily batch job to fetch and update the Net Asset Value (NAV) for all mutual funds and the closing prices for stocks.
- **Search & Discovery APIs:** Provide rich, performant search and filtering APIs for the frontend to allow customers to easily discover investment products (e.g., filter by asset class, risk level, or performance).
- **Oracle DB:** Used to store the structured, relational master data for all investment products.
- **Redis:** Used heavily to cache frequently accessed and fast-changing data, such as daily NAVs and stock prices, ensuring the discovery process is snappy for the user.
- **Publishes to investment.marketdata.events:**
  - **Event:** ProductNAVsUpdated (A daily event that triggers portfolio revaluation).

**5.5.4 Microservice: order-management-service (OMS)**

To serve as the transactional backbone for all investment activities. It reliably processes all buy, sell, and systematic investment orders, acting as the bridge between our platform and external financial exchanges or registrars.

- **Order Processing:** Handle all investment order types: one-time purchases (Lump Sum), redemptions (Sell), and Systematic Investment Plans (SIPs).
- **Pre-Order Validation:** Before placing an order, it validates the customer's InvestmentProfile is active and orchestrates the debit of funds by calling the payment-gateway-service.
- **External Integration:** Integrate with external exchange or registrar APIs to place the actual orders.
- **Order Lifecycle Management:** Manage the status of each order (RECEIVED, FUNDS_DEBITED, SENT_TO_EXCHANGE, EXECUTED, FAILED).
- **SIP Management:** Manage the scheduling and periodic execution of all SIPs via an internal scheduler.
- **Publishes to investment.order.events:**
  - **Event:** InvestmentOrderExecuted (The critical trigger for the portfolio-management-service. Contains details of units allocated and the execution price).

**5.5.5 Microservice: portfolio-management-service**

To provide the customer with a clear, aggregated, and insightful view of their entire investment portfolio. This service is primarily for data aggregation, calculation, and presentation.

- **Holdings Management:** Subscribe to InvestmentOrderExecuted events to maintain an accurate, real-time record of each customer's holdings (e.g., "Customer X owns 125.3 units of ABC Equity Fund").
- **Portfolio Valuation:** Subscribe to the daily ProductNAVsUpdated event to re-calculate the current market value of all customer holdings.
- **Performance Calculation:** Calculate and serve key portfolio metrics via API: total investment cost, current market value, overall profit/loss, and portfolio returns (e.g., XIRR - Extended Internal Rate of Return).
- **Statement Generation:** Generate consolidated portfolio statements for users on demand.

**5.5.6 Microservice: robo-advisory-service**

To house the **Robo-Advisory AI Agent**, which provides automated, personalized investment advice, acting as a "virtual wealth manager."

- **Personalized Recommendation Engine:** Expose a secure API that accepts a customerId and returns personalized investment recommendations.
- **Data-Driven Insights:** The AI Agent will:
  - Fetch the customer's risk profile from the investment-onboarding-service.
  - Optionally, analyze the customer's cash flow and spending patterns (from account-management-service) to suggest an appropriate investment amount.
  - Consider the customer's stated goals (e.g., "Retirement in 20 years").
- **Portfolio Construction:** Utilize financial algorithms (e.g., Modern Portfolio Theory) and ML models to recommend an optimal asset allocation (e.g., "60% Equity, 30% Debt, 10% Gold").
- **Product Matching:** Suggest a specific, pre-approved basket of funds from the product-catalog-service that aligns with the recommended asset allocation.
- **Portfolio Rebalancing:** Periodically analyze the customer's holdings (from portfolio-management-service). If the current allocation has drifted significantly from the target due to market movements, it will publish a RebalancingRecommendationAvailable event to proactively notify the user.

| **Method** | **Path** | **Description** | **Sample Request Body / Response Body** |
| --- | --- | --- | --- |
| **GET** | /api/v1/advisory/recommendations/{customerId} | (User-facing) Gets a personalized investment recommendation. | **Response:** { "targetAllocation": {...}, "recommendedPortfolios": \[...\] } |

**6.0 Cross-Cutting Concerns (Horizontal Capabilities)**

**6.1 Domain: Admin Portal & Operations**

The Admin Portal is the central command center for all OmniBank employees, from customer service representatives to risk officers and auditors. This domain provides the necessary backend services to power a unified, role-based administrative interface. Its primary architectural pattern is the **Backend for Frontend (BFF)**, which decouples the admin UI from the complexities of the downstream microservice ecosystem.

- **Purpose:** To act as the single, dedicated API gateway and aggregation layer for the Angular Admin Portal. It simplifies the frontend's logic by providing coarse-grained, use-case-specific APIs.
- **Core Responsibilities:**
  - **Data Aggregation (Customer 360° View):** When an admin searches for a customer, this service is responsible for making concurrent, parallel API calls to customer-profile-service, account-management-service, loan-management-service, and card-management-service. It then aggregates, transforms, and combines the responses into a single, unified JSON object representing the customer's complete relationship with the bank.
  - **Workflow Orchestration:** It provides endpoints that orchestrate complex administrative actions. For example, a "Freeze Customer Account" request from the admin portal will hit a single endpoint on this service, which will then make authorized calls to customer-profile-service (to update status) and account-management-service (to freeze accounts).
  - **Admin-Specific Security:** It is tightly integrated with Keycloak and is responsible for enforcing fine-grained, role-based access control (RBAC) for the admin portal. It checks that the logged-in administrator has the necessary permissions (e.g., loan_officer) before forwarding a request to a downstream service.
- **Technology:** Built using Spring WebFlux (reactive stack) to efficiently handle a high number of concurrent I/O-bound requests to downstream services.
- **Data Model:** This service is entirely stateless.
- **Purpose:** To offload the resource-intensive task of generating complex, analytical business reports from the live transactional services.
- **Core Responsibilities:**
  - Generate scheduled and on-demand reports that require joining data across multiple business domains (e.g., "Monthly report of all new personal loans approved in the 'West' region with a credit score below 700").
  - Provide endpoints to download reports in various formats, such as PDF and CSV.
  - **Data Source:** This service **must not** query the live transactional (OLTP) databases. It will connect to a dedicated, replicated **reporting database (OLAP)**. This database will be populated via a near-real-time data pipeline using Kafka Connect and Debezium, as described in the Data Management section.
- **Data Model (Oracle DB - Reporting Instance):** A denormalized schema optimized for fast analytical queries.

**6.2 Domain: Security & Identity Management**

- **Purpose:** To serve as the single, centralized authority for all authentication and authorization on the OmniBank platform.
- **Responsibilities:**
  - **User & Admin Authentication:** Manages all user credentials, password policies, and login flows.
  - **Multi-Factor Authentication (MFA):** Enforces MFA using TOTP (e.g., Google Authenticator) or SMS OTPs (via integration with a service like Twilio).
  - **Token Issuance:** Issues digitally signed, short-lived JSON Web Tokens (JWTs) compliant with the OAuth 2.0 and OpenID Connect standards upon successful authentication.
  - **Centralized Role Management:** Defines all roles (retail_customer, corporate_user, loan_officer, auditor) and permissions.
  - **Service Accounts:** Manages identities (Client ID & Secret) for all microservices, enabling secure service-to-service communication.
- **Purpose:** To act as the fortified front door for the entire platform, shielding the internal microservices from direct exposure to the public internet.
- **Core Responsibilities:**
  - **TLS Termination:** Terminates all incoming HTTPS traffic.
  - **Request Routing:** Intelligently routes incoming requests to the appropriate downstream microservice based on the URL path.
  - **JWT Validation (Critical):** Intercepts every single incoming API request. It validates the signature and expiry of the JWT Bearer Token against Keycloak's public keys. **Any request without a valid token is rejected at the gateway and never reaches the internal network.**
  - **Rate Limiting:** Protects backend services from abuse and Denial-of-Service attacks.
  - **CORS Policy Enforcement:** Manages Cross-Origin Resource Sharing policies centrally.

**6.3 Domain: Observability**

- **Purpose:** To provide a centralized, searchable, real-time logging platform for the entire distributed system.
- **Architecture:**
  - **Log Generation:** Every microservice is configured with a Logback/Log4j2 appender that writes logs in a structured **JSON format**.
  - **Correlation ID:** The Spring Cloud Gateway generates a unique correlationId (e.g., a UUID) for every incoming request and adds it as an HTTP header. This header is automatically propagated to all subsequent downstream service calls. Every single log line written during the lifecycle of that request will contain this correlationId.
  - **Log Shipping (Logstash/Fluentd):** A lightweight agent collects these JSON logs from all running service instances.
  - **Indexing & Storage (Elasticsearch):** A powerful search engine that indexes the logs.
  - **Visualization (Kibana):** A web UI that allows developers to search, filter, and visualize logs. A developer can query for a specific correlationId to see the complete, ordered story of a request as it flowed through multiple microservices.
- **Purpose:** To collect, store, and visualize time-series metrics to understand the health and performance of the platform in real-time.
- **Architecture:**
  - **Metrics Exposition:** Every Spring Boot microservice includes the Micrometer library, which exposes a /actuator/prometheus endpoint with hundreds of metrics (JVM health, request latency, error counts, connection pool stats, etc.).
  - **Metrics Collection (Prometheus):** A Prometheus server is configured to periodically "scrape" these endpoints from every running service instance.
  - **Visualization & Alerting (Grafana):** Grafana connects to Prometheus as a data source. We will build a suite of dashboards for:
    - **Global Overview:** High-level health of the entire platform.
    - **Service-Specific Dashboards:** Deep-dive into the performance of a single microservice.
    - **Business Dashboards:** Visualizing KPIs like transactions per minute or new user sign-ups.
  - **Alertmanager** will be configured to send alerts (e.g., to Slack or PagerDuty) when critical thresholds are breached.
- **Purpose:** To provide deep insight into the latency of requests as they traverse the distributed system, allowing for the precise identification of performance bottlenecks.
- **Architecture:**
  - **Trace Generation:** The OpenTelemetry Java agent is attached to each microservice's JVM. It automatically instruments popular libraries (Spring Web, JDBC, Kafka clients) to generate and propagate trace data.
  - **Trace Collection:** A Jaeger Collector receives this trace data from all agents.
  - **Visualization:** The Jaeger UI allows developers to view a "flame graph" of a request. This visualizes the entire call chain, showing exactly how much time was spent in each service and in each operation within that service (e.g., a database call), making it trivial to spot the source of latency.

**7.0 Data Management & Persistence Strategy**

**7.1 Guiding Principles**

- **Polyglot Persistence:** The platform will deliberately use multiple database technologies. Each microservice will use a persistence technology best suited to its specific data structure, access patterns, and consistency requirements. This is a core tenet of the microservice architecture.
- **Data Ownership:** Each microservice is the sole owner and arbiter of its own data. No other service is allowed to directly access another service's database. All data access must occur through the service's public API contract. This prevents the creation of a distributed monolith and ensures data integrity.
- **Separation of OLTP and OLAP:** Online Transaction Processing (OLTP) workloads are the day-to-day operations of the bank. Online Analytical Processing (OLAP) workloads are the business intelligence and reporting queries. These two workloads have fundamentally different performance characteristics and **must be physically separated** to ensure that analytical queries never impact the performance of core customer-facing operations.

**7.2 Database Technologies & Usage**

- **Purpose:** To serve as the master system of record for all highly structured, relational, and transactional data where ACID compliance is non-negotiable.
- **Services Using Oracle:** customer-profile-service, account-management-service, ledger-service, loan-management-service, card-management-service, portfolio-management-service, and others requiring strong transactional guarantees.
- **Key Enterprise Features to be Utilized:**
  - **Real Application Clusters (RAC):** To provide high availability and active-active scalability at the database layer for mission-critical services like the ledger.
  - **Transparent Data Encryption (TDE):** To encrypt data at rest, protecting sensitive customer and financial data directly on the filesystem.
  - **Fine-Grained Auditing (FGA):** To create detailed, policy-based audit trails for all access to sensitive tables (e.g., log every SELECT on the CUSTOMERS table).
  - **Connection Pooling:** All microservices will use a high-performance connection pool (HikariCP) to efficiently manage sessions with the Oracle database.
- **Purpose:** To store semi-structured, document-centric data related to stateful workflows or "case files." Its flexible schema is ideal for this purpose.
- **Services Using MongoDB:** customer-onboarding-service, loan-origination-service, card-issuance-service.
- **Schema Design:** While flexible, schemas will be enforced at the application layer via Data Transfer Objects (DTOs) and Object-Document Mappers (ODMs) to maintain data consistency and quality.
- **Purpose:** To provide sub-millisecond latency for read operations, significantly reducing the load on primary databases and improving the responsiveness of the platform.
- **Services Using Redis:**
  - card-authorization-service: For caching card status, limits, and balances. This is a critical performance requirement.
  - api-gateway: For rate limiting and session management.
  - product-catalog-service: For caching daily market data (NAVs, stock prices).
- **Caching Strategy:** The **Cache-Aside Pattern** will be the standard implementation. A service will first check Redis for the required data. On a cache miss, it will query the primary database, retrieve the data, and then populate the Redis cache with a defined Time-To-Live (TTL).

**7.3 Reporting & Analytics Data Pipeline (OLAP Strategy)**

This pipeline is designed to move data from the live OLTP systems into a dedicated reporting environment in near-real-time.

- **Architecture:**
  - **Change Data Capture (CDC):** **Debezium connectors for Oracle and MongoDB** will be deployed to the Kafka Connect framework. Debezium will monitor the database transaction logs (redo logs in Oracle) and capture every single committed INSERT, UPDATE, and DELETE operation as a structured event.
  - **Event Streaming (Kafka):** Debezium will publish these change events to dedicated Kafka topics (e.g., db.customer_master.customers, db.account_master.accounts). This creates a real-time stream of all data changes occurring across the entire platform.
  - **Data Sink:** A **Kafka Connect JDBC Sink connector** will subscribe to these topics.
  - **Loading:** The Sink connector will write these events into a separate **Oracle Database instance, optimized for analytical workloads** (the reporting database). This database can have a denormalized schema (e.g., star schema) to accelerate report generation.
- **Benefit:** This architecture completely isolates the reporting-service from the live databases, guaranteeing that even the most complex, long-running query from an analyst will have zero impact on a customer trying to make a payment.

**8.0 Deployment & CI/CD Strategy**

**8.1 Guiding Principles**

- **Automation:** Every step from code commit to production deployment will be automated. Manual intervention will be limited to explicit approvals for production releases.
- **Infrastructure as Code (IaC):** Server configurations, network rules, and application deployments will be defined in code using **Ansible**. This ensures environments are consistent, repeatable, and auditable.
- **Immutable Deployments:** We do not modify running servers. A new release is deployed by provisioning new server instances with the updated application artifact and then shifting traffic to them. The old instances are then decommissioned.
- **Continuous Integration (CI):** Every code commit to a feature branch will automatically trigger a build and a comprehensive suite of automated tests.
- **Continuous Delivery (CD):** Once a build passes all tests in the CI stage, a release artifact will be automatically created and deployed to a staging environment. Deployment to production will be a one-click, manually-approved action.

**8.2 The CI/CD Pipeline (Jenkins)**

The pipeline will be defined in a Jenkinsfile and will consist of the following stages:

- **Checkout:** Pulls the latest source code from the Git repository.
- **Build:** Compiles the Java code using Maven, resolves dependencies, and creates a JAR artifact.
- **Unit & Integration Test:** Runs a comprehensive suite of JUnit tests and Spring Boot integration tests. Test coverage thresholds will be enforced.
- **Code Quality & Security Scan (SonarQube):** The code is statically analyzed by SonarQube to identify bugs, code smells, and critical security vulnerabilities (e.g., SQL injection, hardcoded secrets). The pipeline will fail if the defined "Quality Gate" is not passed.
- **Build Artifact:** If all previous stages pass, the JAR is versioned and pushed to a central artifact repository (e.g., JFrog Artifactory or Nexus).
- **Deploy to Staging:** An Ansible playbook is triggered. It provisions the necessary infrastructure, pulls the new JAR artifact, and deploys it to the staging environment, which is a mirror of production.
- **Automated E2E & Performance Tests:** A suite of end-to-end tests is run against the staging environment to validate key user journeys. Basic performance tests may also be triggered.
- **Approval Gate:** The pipeline pauses and waits for manual approval from the release manager or a product owner to proceed with the production deployment.
- **Deploy to Production (Rolling Deployment):** Once approved, the Ansible playbook executes a **rolling deployment strategy** on the production environment.
  - It deploys the new version to a small subset of servers.
  - It waits for health checks to pass.
  - It removes the old version from those servers.
  - It repeats this process iteratively across the entire server fleet, ensuring that the application remains available throughout the deployment process with zero downtime.

**9.0 Governance, Operations & Future Strategy**

**9.1 Architectural Governance**

To maintain the integrity and consistency of the microservices architecture as the team and the platform scale, a lightweight but effective governance model will be established. The goal is not to create bureaucracy but to ensure that core principles are upheld and technical debt is managed proactively.

- **Mandate:** A small group comprising the Chief Architect, senior developers from key domains, and a representative from the DevOps/SRE team.
- **Responsibilities:**
  - **Reviewing New Services:** Any proposal for a new microservice must be briefly reviewed by the ARB to ensure it has a clear, bounded context and doesn't duplicate existing functionality.
  - **Defining "Golden Paths":** The ARB is responsible for maintaining and updating the "golden path" starter templates for new services. These templates will come pre-configured with logging, metrics, security, and CI/CD pipeline definitions.
  - **Technology Radar:** Maintain a living document (a "Technology Radar") that tracks which technologies are in use, which are recommended for new projects, which are being trialed, and which should be retired. This prevents technological sprawl.
  - **API Contract Reviews:** Enforce API design standards (e.g., consistent error handling, versioning strategy) to ensure interoperability.

A consistent API versioning strategy is crucial for allowing services to evolve without breaking their consumers.

- **Strategy:** URI-based versioning will be used (e.g., /api/v1/..., /api/v2/...).
- **Policy:**
  - **Non-Breaking Changes** (e.g., adding a new optional field to a JSON response) can be made to an existing API version.
  - **Breaking Changes** (e.g., removing a field, changing a data type, altering the URL structure) **must** result in a new API version (e.g., /v2/).
  - A clear deprecation policy will be established. When a v2 is released, v1 will be supported for a defined period (e.g., 6 months) to give consumers time to migrate.

**9.2 Operational Readiness & Site Reliability Engineering (SRE)**

The SRE team's role is to ensure the platform is reliable, scalable, and efficient. They will work closely with development teams, applying software engineering principles to operations.

Instead of vague goals, every critical service will have defined SLOs.

- **SLO Definition:** An SLO is a target value for a specific reliability metric. For example:
  - **payment-gateway-service Availability SLO:** 99.95% of requests over a rolling 28-day period will be successful (return a non-5xx status code).
  - **card-authorization-service Latency SLO:** 99% of authorization requests will be completed in under 100ms.
- **Error Budgets:** Each SLO creates an "error budget" - the amount of unreliability that is acceptable. For a 99.95% availability SLO, the error budget is 0.05%. This budget empowers development teams. If the service is operating well within its budget, they can release new features faster. If the budget is being consumed, feature releases must be halted to focus on stability.
- **On-Call Rotations:** An on-call rotation will be established, with clear escalation paths.
- **Blameless Post-mortems:** After every significant incident, a blameless post-mortem will be conducted. The focus is never on "who" caused the issue, but on "what" in the system and processes allowed the issue to occur, and what concrete actions can be taken to prevent it from happening again.

**9.3 Future Strategy & Architectural Evolution**

This architecture is designed not as a final state but as a foundation for future evolution. The following are key areas for future development.

- **From Automation to Proaction:** The initial AI agents are focused on automating existing tasks. The next evolution will be to make them proactive.
  - **Proactive Financial Advisor:** The Robo-Advisory agent could evolve to monitor a customer's financial health and proactively send alerts and suggestions, e.g., "We noticed you have a high balance in your savings account. You could be earning more by moving a portion to a low-risk fixed deposit."
  - **Dynamic Fraud Models:** The fraud detection model will be continuously retrained on new transaction data, automatically adapting to emerging fraud patterns.
- **Hyper-Personalization:** An AI agent will be dedicated to personalizing the user experience, from custom-tailoring the UI to presenting the most relevant products and offers based on the user's behavior and life events (inferred from their financial data).

For certain critical domains like the ledger-service, a future iteration could evolve from a state-based persistence model to an **Event Sourcing** model.

- **Concept:** Instead of storing the current state of an account (the balance), we would only store the stream of events that have happened to it (AccountOpened, FundsDeposited, FundsWithdrawn). The current state would be derived by replaying these events.
- **Benefits:** This provides a perfect, immutable audit log of everything that has ever happened. It allows for advanced analytics and makes it possible to debug issues by replaying history.

As the number of microservices grows beyond 20-30, managing network communication, security, and observability can become complex. A **Service Mesh** (e.g., Istio or Linkerd) could be introduced.

- **Concept:** A service mesh injects a lightweight "sidecar" proxy next to each microservice instance. All network traffic flows through this proxy.
- **Benefits:**
  - **Advanced Traffic Management:** Provides out-of-the-box support for canary deployments, A/B testing, and circuit breakers without any changes to the application code.
  - **Enhanced Security:** Enforces mutual TLS (mTLS) for all internal service-to-service communication automatically.
  - **Deeper Observability:** Provides consistent, detailed metrics and traces for all network traffic between services.

\==================================================================