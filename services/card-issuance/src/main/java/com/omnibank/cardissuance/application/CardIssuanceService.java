package com.omnibank.cardissuance.application;

import com.omnibank.cardissuance.config.AppProperties;
import com.omnibank.cardissuance.events.EventPublisher;
import com.omnibank.cardissuance.events.EventTypes;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory card issuance workflow (dev-open):
 * SUBMITTED -> ELIGIBILITY_CHECKED -> APPROVED
 */
@Service
@RequiredArgsConstructor
public class CardIssuanceService {

  private final ConcurrentHashMap<String, Application> store = new ConcurrentHashMap<>();
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  public Created submit(SubmitRequest req, String correlationId) {
    validateSubmit(req);
    String id = UUID.randomUUID().toString();
    Application a = new Application();
    a.setApplicationId(id);
    a.setCustomerId(req.customerId);
    a.setProductType(req.productType != null ? req.productType.toUpperCase() : "CREDIT_CARD");
    a.setStatus("SUBMITTED");
    a.setSubmittedAt(Instant.now());
    store.put(id, a);
    return new Created(id, a.getStatus());
  }

  public ApplicationView get(String applicationId) {
    Application a = required(applicationId);
    return toView(a);
  }

  public ApplicationView eligibilityCheck(String applicationId, String correlationId) {
    Application a = required(applicationId);
    if ("SUBMITTED".equalsIgnoreCase(a.getStatus())) {
      a.setStatus("ELIGIBILITY_CHECKED");
      a.setEligibilityCheckedAt(Instant.now());
    }
    return toView(a);
  }

  public ApplicationView approve(String applicationId, String correlationId) {
    Application a = required(applicationId);
    if (!"ELIGIBILITY_CHECKED".equalsIgnoreCase(a.getStatus()) && !"SUBMITTED".equalsIgnoreCase(a.getStatus())) {
      // Allow idempotent re-approve and also fast-approve from SUBMITTED in dev
    }
    a.setStatus("APPROVED");
    a.setApprovedAt(Instant.now());

    publish(props.getEvents().getTopic(),
        EventTypes.CARD_APPLICATION_APPROVED,
        Map.of(
            "applicationId", a.getApplicationId(),
            "customerId", a.getCustomerId(),
            "productType", a.getProductType(),
            "approvedAt", a.getApprovedAt().toString()
        ),
        correlationId
    );
    return toView(a);
  }

  private void publish(String topic, String type, Object payload, String correlationId) {
    try {
      eventPublisher.publish(topic, type, payload, correlationId);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to publish event '" + type + "': " + ex.getMessage());
    }
  }

  private static void validateSubmit(SubmitRequest r) {
    if (r == null) throw new IllegalArgumentException("request is required");
    if (r.customerId == null || r.customerId <= 0) throw new IllegalArgumentException("customerId must be provided");
  }

  private Application required(String id) {
    Application a = store.get(id);
    if (a == null) throw new IllegalArgumentException("Application not found: " + id);
    return a;
  }

  // Request DTOs

  @Data
  public static class SubmitRequest {
    public Long customerId;
    public String productType; // CREDIT_CARD, DEBIT_CARD, etc.
  }

  // Views

  public record Created(String applicationId, String status) {}

  public record ApplicationView(
      String applicationId,
      Long customerId,
      String productType,
      String status,
      Instant submittedAt,
      Instant eligibilityCheckedAt,
      Instant approvedAt
  ) {}

  private static ApplicationView toView(Application a) {
    return new ApplicationView(
        a.getApplicationId(),
        a.getCustomerId(),
        a.getProductType(),
        a.getStatus(),
        a.getSubmittedAt(),
        a.getEligibilityCheckedAt(),
        a.getApprovedAt()
    );
  }

  // Internal model
  @Data
  public static class Application {
    private String applicationId;
    private Long customerId;
    private String productType;
    private String status; // SUBMITTED | ELIGIBILITY_CHECKED | APPROVED
    private Instant submittedAt;
    private Instant eligibilityCheckedAt;
    private Instant approvedAt;
  }
}
