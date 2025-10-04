package com.omnibank.cardmanagement.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibank.cardmanagement.application.CardManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for card issuance domain events.
 * Listens to card-issuance events and auto-creates a card in card-management when an application is approved.
 * Idempotency is enforced inside CardManagementService via issuanceMarkers keyed by applicationId.
 */
@Component
@ConditionalOnProperty(prefix = "card-management.kafka", name = "enabled", havingValue = "true")
public class KafkaIssuanceConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaIssuanceConsumer.class);
  private static final String TYPE_CARD_APPLICATION_APPROVED = "CardApplicationApproved";

  private final CardManagementService service;
  private final ObjectMapper mapper = new ObjectMapper();

  public KafkaIssuanceConsumer(CardManagementService service) {
    this.service = service;
  }

  @KafkaListener(topics = "${card-management.events.issuanceTopic:card.issuance.events}")
  public void onIssuanceEvent(String message) {
    try {
      JsonNode root = mapper.readTree(message);
      String type = asText(root, "type");
      String correlationId = asText(root, "correlationId");
      if (TYPE_CARD_APPLICATION_APPROVED.equals(type)) {
        JsonNode payload = root.path("payload");
        String applicationId = asText(payload, "applicationId");
        Long customerId = asLong(payload, "customerId");
        String productType = asText(payload, "productType");

        log.info("Consuming {} applicationId={} customerId={} productType={}",
            TYPE_CARD_APPLICATION_APPROVED, applicationId, customerId, productType);

        service.createFromIssuanceApproval(applicationId, customerId, productType, correlationId);
      } else {
        // Unknown or unhandled type: log and ignore (extensible for future types)
        log.debug("Ignoring event type={} (not handled by card-management)", type);
      }
    } catch (Exception ex) {
      // In local dev, log and rethrow to allow Kafka retry semantics if configured.
      log.warn("Failed to process issuance event: {}", ex.getMessage(), ex);
      throw new IllegalStateException("Failed to process issuance event", ex);
    }
  }

  private static String asText(JsonNode node, String field) {
    JsonNode v = node.get(field);
    return v == null || v.isNull() ? null : v.asText();
  }

  private static Long asLong(JsonNode node, String field) {
    JsonNode v = node.get(field);
    if (v == null || v.isNull()) return null;
    try {
      return v.asLong();
    } catch (Exception e) {
      return null;
    }
  }
}
