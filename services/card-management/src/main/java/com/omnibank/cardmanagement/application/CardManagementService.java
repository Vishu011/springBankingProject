package com.omnibank.cardmanagement.application;

import com.omnibank.cardmanagement.config.AppProperties;
import com.omnibank.cardmanagement.events.EventPublisher;
import com.omnibank.cardmanagement.events.EventTypes;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory card management (dev-open):
 * - Create (dev-only), Activate, Status (BLOCK/UNBLOCK), Limits
 * - Publishes events via logging publisher.
 */
@Service
@RequiredArgsConstructor
public class CardManagementService {

  private final ConcurrentHashMap<String, Card> store = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> issuanceMarkers = new ConcurrentHashMap<>();
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  public Created createDev(CreateCardRequest req, String correlationId) {
    validateCreate(req);
    String cardId = UUID.randomUUID().toString();
    String panRef = "PANREF-" + cardId.substring(0, 8);
    String last4 = randomLast4();

    Card c = new Card();
    c.setCardId(cardId);
    c.setCustomerId(req.customerId);
    c.setProductType(nvlUpper(req.productType, "CREDIT_CARD"));
    c.setStatus("PENDING");
    c.setPanRef(panRef);
    c.setLast4(last4);
    c.setSpendLimit(nvl(req.initialLimit, new BigDecimal("1000.00")));
    c.setCreatedAt(Instant.now());

    store.put(cardId, c);

    publish(EventTypes.CARD_CREATED, Map.of(
        "cardId", c.getCardId(),
        "customerId", c.getCustomerId(),
        "productType", c.getProductType(),
        "last4", c.getLast4(),
        "createdAt", c.getCreatedAt().toString()
    ), correlationId);

    return new Created(cardId, c.getStatus());
  }

  public CardView get(String cardId) {
    return toView(required(cardId));
  }

  public CardView activate(String cardId, String correlationId) {
    Card c = required(cardId);
    if ("BLOCKED".equalsIgnoreCase(c.getStatus())) {
      throw new IllegalStateException("Cannot activate a blocked card; unblock first");
    }
    if (!"ACTIVE".equalsIgnoreCase(c.getStatus())) {
      c.setStatus("ACTIVE");
      c.setActivatedAt(Instant.now());
      publish(EventTypes.CARD_STATUS_UPDATED, Map.of(
          "cardId", c.getCardId(),
          "status", c.getStatus(),
          "activatedAt", c.getActivatedAt().toString()
      ), correlationId);
    }
    return toView(c);
  }

  public CardView updateStatus(String cardId, UpdateStatusRequest req, String correlationId) {
    if (req == null || req.status == null || req.status.isBlank()) {
      throw new IllegalArgumentException("status required (BLOCK|UNBLOCK)");
    }
    String normalized = req.status.toUpperCase();
    if (!normalized.equals("BLOCK") && !normalized.equals("UNBLOCK")) {
      throw new IllegalArgumentException("status must be BLOCK or UNBLOCK");
    }
    Card c = required(cardId);
    c.setStatus(normalized.equals("BLOCK") ? "BLOCKED" : "ACTIVE");
    publish(EventTypes.CARD_STATUS_UPDATED, Map.of(
        "cardId", c.getCardId(),
        "status", c.getStatus()
    ), correlationId);
    return toView(c);
  }

  public CardView updateLimits(String cardId, UpdateLimitsRequest req, String correlationId) {
    if (req == null || req.limit == null || req.limit.compareTo(new BigDecimal("0.00")) <= 0) {
      throw new IllegalArgumentException("limit must be > 0");
    }
    Card c = required(cardId);
    c.setSpendLimit(req.limit);
    publish(EventTypes.CARD_LIMITS_CHANGED, Map.of(
        "cardId", c.getCardId(),
        "limit", c.getSpendLimit()
    ), correlationId);
    return toView(c);
  }

  public CardView createFromIssuanceApproval(String applicationId, Long customerId, String productType, String correlationId) {
    if (applicationId == null || applicationId.isBlank()) {
      throw new IllegalArgumentException("applicationId is required");
    }
    if (customerId == null || customerId <= 0) {
      throw new IllegalArgumentException("customerId must be provided");
    }
    String existingCardId = issuanceMarkers.get(applicationId);
    if (existingCardId != null) {
      return toView(required(existingCardId));
    }
    CreateCardRequest req = new CreateCardRequest();
    req.customerId = customerId;
    req.productType = productType;
    Created created = createDev(req, correlationId);
    issuanceMarkers.putIfAbsent(applicationId, created.cardId());
    return get(created.cardId());
  }

  // DTOs

  @Data
  public static class CreateCardRequest {
    @NotNull
    public Long customerId;
    public String productType; // CREDIT_CARD, DEBIT_CARD, etc.

    @DecimalMin(value = "0.01", message = "initialLimit must be > 0")
    public BigDecimal initialLimit;
  }

  @Data
  public static class UpdateStatusRequest {
    @NotBlank
    public String status; // BLOCK|UNBLOCK
  }

  @Data
  public static class UpdateLimitsRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "limit must be > 0")
    public BigDecimal limit;
  }

  public record Created(String cardId, String status) {}

  public record CardView(
      String cardId,
      Long customerId,
      String productType,
      String status,
      String last4,
      BigDecimal spendLimit,
      Instant createdAt,
      Instant activatedAt
  ) {}

  // Internal model
  @Data
  public static class Card {
    private String cardId;
    private Long customerId;
    private String productType;
    private String status; // PENDING|ACTIVE|BLOCKED
    private String panRef; // masked/pan ref (not a PAN)
    private String last4;
    private BigDecimal spendLimit;
    private Instant createdAt;
    private Instant activatedAt;
  }

  // Helpers

  private static void validateCreate(CreateCardRequest r) {
    if (r == null) throw new IllegalArgumentException("request is required");
    if (r.customerId == null || r.customerId <= 0) throw new IllegalArgumentException("customerId must be provided");
    if (r.initialLimit != null && r.initialLimit.compareTo(new BigDecimal("0.00")) <= 0) {
      throw new IllegalArgumentException("initialLimit must be > 0");
    }
  }

  private static CardView toView(Card c) {
    return new CardView(
        c.getCardId(),
        c.getCustomerId(),
        c.getProductType(),
        c.getStatus(),
        c.getLast4(),
        c.getSpendLimit(),
        c.getCreatedAt(),
        c.getActivatedAt()
    );
  }

  private Card required(String id) {
    Card c = store.get(id);
    if (c == null) throw new IllegalArgumentException("Card not found: " + id);
    return c;
  }

  private void publish(String type, Object payload, String correlationId) {
    try {
      eventPublisher.publish(props.getEvents().getTopic(), type, payload, correlationId);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to publish event '" + type + "': " + ex.getMessage());
    }
  }

  private static String randomLast4() {
    SecureRandom rnd = new SecureRandom();
    int n = rnd.nextInt(10000);
    return String.format("%04d", n);
    }

  private static String nvlUpper(String v, String d) {
    return (v == null || v.isBlank()) ? d : v.toUpperCase();
  }

  private static BigDecimal nvl(BigDecimal v, BigDecimal d) {
    return Objects.requireNonNullElse(v, d);
  }
}
