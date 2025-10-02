package com.omnibank.loanorigination.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/**
 * Minimal in-memory implementation for dev-local (no Mongo required).
 * Provides basic application lifecycle to unblock API wiring and Postman flows.
 * Later: swap to Mongo repository + event publication (LoanApplicationSubmitted / LoanApproved / LoanDisbursed).
 */
@Service
public class LoanOriginationService {

  private final ConcurrentMap<String, Application> store = new ConcurrentHashMap<>();

  public Created createApplication(StartParams params, String correlationId) {
    validateStart(params);
    String id = UUID.randomUUID().toString();
    Application app = new Application();
    app.applicationId = id;
    app.status = "STARTED";
    app.loanType = params.loanType != null ? params.loanType.toUpperCase() : "PERSONAL";
    app.requestedAmount = params.amount;
    app.createdAt = Instant.now();

    // For dev-local: append a history note
    app.underwritingHistory = new ArrayList<>();
    app.underwritingHistory.add(new History("INIT", "SYSTEM", Instant.now(), "STARTED", null));

    store.put(id, app);
    return new Created(id, app.status);
  }

  public ApplicationView getApplication(String applicationId) {
    Application app = getRequired(applicationId);
    return toView(app);
  }

  /**
   * Record a final decision. APPROVED or REJECTED.
   * If APPROVED, approvedAmount and interestRate must be provided.
   */
  public void recordDecision(DecisionParams params, String correlationId) {
    validateDecision(params);

    Application app = getRequired(params.applicationId);
    if (!Objects.equals(app.status, "STARTED")) {
      // allow idempotent re-POST of same final decision
      if (Objects.equals(app.status, params.decision)) {
        return;
      }
      throw new IllegalStateException("Application is not in a state to record decision: " + app.status);
    }

    if ("APPROVED".equalsIgnoreCase(params.decision)) {
      app.status = "APPROVED";
      app.approvedAmount = params.approvedAmount;
      app.interestRate = params.interestRate;
      app.decisionBy = params.decisionBy != null ? params.decisionBy : "SYSTEM";
    } else if ("REJECTED".equalsIgnoreCase(params.decision)) {
      app.status = "REJECTED";
      app.decisionBy = params.decisionBy != null ? params.decisionBy : "SYSTEM";
    } else {
      throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
    }

    if (app.underwritingHistory == null) {
      app.underwritingHistory = new ArrayList<>();
    }
    app.underwritingHistory.add(new History(
        "FINAL_DECISION",
        app.decisionBy,
        Instant.now(),
        app.status,
        null
    ));

    // FUTURE: publish LoanApproved event when APPROVED (logging publisher by default)
  }

  // Params and DTO views

  public record StartParams(String loanType, BigDecimal amount) {}

  public record DecisionParams(
      String applicationId,
      String decision,            // APPROVED | REJECTED
      BigDecimal approvedAmount,  // required if APPROVED
      BigDecimal interestRate,    // required if APPROVED
      String decisionBy
  ) {}

  public record Created(String applicationId, String status) {}

  public record ApplicationView(
      String applicationId,
      String status,
      String loanType,
      BigDecimal requestedAmount,
      BigDecimal approvedAmount,
      BigDecimal interestRate,
      String decisionBy,
      Instant createdAt,
      List<History> underwritingHistory
  ) {}

  public record History(
      String stage,
      String actor,
      Instant timestamp,
      String outcome,
      String details
  ) {}

  // Internal model
  static class Application {
    String applicationId;
    String status;           // STARTED | APPROVED | REJECTED
    String loanType;
    BigDecimal requestedAmount;

    BigDecimal approvedAmount;
    BigDecimal interestRate;
    String decisionBy;

    Instant createdAt;
    List<History> underwritingHistory;
  }

  private Application getRequired(String id) {
    Application app = store.get(id);
    if (app == null) {
      throw new IllegalArgumentException("Application not found: " + id);
    }
    return app;
  }

  private static ApplicationView toView(Application a) {
    return new ApplicationView(
        a.applicationId,
        a.status,
        a.loanType,
        a.requestedAmount,
        a.approvedAmount,
        a.interestRate,
        a.decisionBy,
        a.createdAt,
        a.underwritingHistory != null ? List.copyOf(a.underwritingHistory) : List.of()
    );
  }

  private static void validateStart(StartParams p) {
    if (p == null) throw new IllegalArgumentException("request is required");
    if (p.amount == null || p.amount.compareTo(new BigDecimal("0.00")) <= 0) {
      throw new IllegalArgumentException("amount must be greater than 0");
    }
  }

  private static void validateDecision(DecisionParams p) {
    if (p == null) throw new IllegalArgumentException("request is required");
    if (p.applicationId == null || p.applicationId.isBlank()) {
      throw new IllegalArgumentException("applicationId is required");
    }
    if (p.decision == null || p.decision.isBlank()) {
      throw new IllegalArgumentException("decision is required");
    }
    if ("APPROVED".equalsIgnoreCase(p.decision)) {
      if (p.approvedAmount == null || p.approvedAmount.compareTo(new BigDecimal("0.00")) <= 0) {
        throw new IllegalArgumentException("approvedAmount must be > 0 for APPROVED decision");
      }
      if (p.interestRate == null || p.interestRate.compareTo(new BigDecimal("0.00")) <= 0) {
        throw new IllegalArgumentException("interestRate must be > 0 for APPROVED decision");
      }
    }
  }
}
