package com.omnibank.paymentgateway.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibank.paymentgateway.domain.PaymentRequest;
import com.omnibank.paymentgateway.repository.PaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes ledger.events TransactionPosted to complete payments asynchronously.
 * Requires kafka profile/properties (payment-gateway.kafka.enabled=true).
 *
 * Expects envelope JSON with optional metadata containing "paymentUuid":
 * {
 *   "topic":"ledger.events",
 *   "type":"TransactionPosted",
 *   "timestamp":"...",
 *   "correlationId":"...",
 *   "payload": {
 *     "transactionId":"...",
 *     "entries":[{"account":"...","amount":123.45,"direction":"D" | "C"}, ...],
 *     "metadata": {"paymentUuid":"..."}
 *   }
 * }
 */
@Component
@ConditionalOnProperty(prefix = "payment-gateway.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LedgerEventsConsumer {

  private final ObjectMapper mapper = new ObjectMapper();
  private final PaymentRequestRepository repository;

  @KafkaListener(topics = "${payment-gateway.events.ledgerTopic:ledger.events}", groupId = "payment-gateway")
  @Transactional
  public void onLedgerEvent(
      String message,
      @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
      @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
      @Header(name = KafkaHeaders.OFFSET, required = false) Long offset
  ) {
    try {
      JsonNode root = mapper.readTree(message);
      String type = text(root, "type");
      if (!"TransactionPosted".equalsIgnoreCase(type)) {
        return;
      }

      JsonNode payload = root.path("payload");
      JsonNode metadata = payload.path("metadata");
      String paymentUuid = text(metadata, "paymentUuid");
      if (paymentUuid == null || paymentUuid.isBlank()) {
        log.debug("TransactionPosted missing metadata.paymentUuid; skipping payment status update. raw={}", message);
        return;
      }

      var prOpt = repository.findByPaymentUuid(paymentUuid);
      if (prOpt.isEmpty()) {
        log.debug("PaymentRequest not found for paymentUuid={}, skipping (topic={}, key={}, offset={})", paymentUuid, topic, key, offset);
        return;
      }

      PaymentRequest pr = prOpt.get();
      if ("COMPLETED".equalsIgnoreCase(pr.getStatus())) {
        log.debug("PaymentRequest already COMPLETED, paymentUuid={}, skipping", paymentUuid);
        return;
      }

      pr.setStatus("COMPLETED");
      repository.save(pr);

      log.info("Marked payment COMPLETED for paymentUuid={} (topic={}, key={}, offset={})", paymentUuid, topic, key, offset);
    } catch (Exception ex) {
      log.error("Failed to process ledger TransactionPosted for payment-gateway (topic={}, key={}, offset={}): {}", topic, key, offset, ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  private static String text(JsonNode node, String field) {
    if (node == null) return null;
    JsonNode n = node.path(field);
    return (n.isMissingNode() || n.isNull()) ? null : n.asText();
  }
}
