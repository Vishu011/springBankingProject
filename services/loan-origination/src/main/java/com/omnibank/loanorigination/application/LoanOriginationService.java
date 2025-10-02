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
import com.omnibank.loanorigination.config.AppProperties;
import com.omnibank.loanorigination.events.EventPublisher;
import com.omnibank.loanorigination.events.EventTypes;
import lombok.RequiredArgsConstructor;

/**
 * Minimal in-memory implementation for dev-local (no Mongo required).
 * Provides basic application lifecycle to unblock API wiring and Postman flows.
 * Later: swap to Mongo repository + event publication (LoanApplicationSubmitted / LoanApproved / LoanDisbursed).
 */
@Service
@RequiredArgsConstructor
public class LoanOriginationService {

  private final ConcurrentMap<String, Application> store = new ConcurrentHashMap<>();
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  public Created createApplication(StartParams params, String correlationId) {
    validateStart(params);
    String id = UUID.randomUUID().toString();
    Application app = new Application();
    app.applicationId = id;
    app.status = "STARTED";
    app.customerId = params.customerId;
    app.loanType = params.loanType != null ? params.loanType.toUpperCase() : "PERSONAL";
    app.requestedAmount = params.amount;
    app.createdAt = Instant.now();

    // For dev-local: append a history note
    app.underwritingHistory = new ArrayList<>();
    app.underwritingHistory.add(new History("INIT", "SYSTEM", Instant.now(), "STARTED", null));

    store.put(id, app);

    // Publish LoanApplicationSubmitted (logging by default; Kafka later)
    publish(
        EventTypes.LOAN_APPLICATION_SUBMITTED,
        new EventPayloads.LoanApplicationSubmitted(
            app.applicationId,
            app.loanType,
            app.requestedAmount,
            app.createdAt
        ),
        correlationId
    );

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

    // Publish LoanApproved when APPROVED (logging by default; Kafka later)
    if ("APPROVED".equalsIgnoreCase(app.status)) {
      publish(
          EventTypes.LOAN_APPROVED,
          new EventPayloads.LoanApproved(
              app.applicationId,
              app.customerId,
              app.loanType,
              app.approvedAmount,
              app.interestRate,
              app.decisionBy
          ),
          correlationId
      );
    }
  }

  private void publish(String type, Object payload, String correlationId) {
    eventPublisher.publish(props.getEvents().getTopic(), type, payload, correlationId);
  }

  // Params and DTO views

  public record StartParams(Long customerId, String loanType, BigDecimal amount) {}

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

  public static class EventPayloads {
    public record LoanApplicationSubmitted(
        String applicationId,
        String loanType,
        BigDecimal requestedAmount,
        Instant createdAt
    ) {}

    public record LoanApproved(
        String applicationId,
        Long customerId,
        String loanType,
        BigDecimal approvedAmount,
        BigDecimal interestRate,
        String decisionBy
    ) {}

    public record LoanDisbursed(
        String applicationId,
        String loanType,
        BigDecimal disbursedAmount,
        String fromAccount,
        String toAccount,
        Instant disbursedAt
    ) {}
  }

  // Internal model
  static class Application {
    String applicationId;
    String status;           // STARTED | APPROVED | REJECTED
    Long customerId;
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
    if (p.customerId == null || p.customerId <= 0L) {
      throw new IllegalArgumentException("customerId must be provided");
    }
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
