package com.omnibank.loanmanagement.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibank.loanmanagement.application.LoanManagementService;
import com.omnibank.loanmanagement.application.LoanManagementService.LoanApprovedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for loan.origination.events -> LoanApproved.
 * Creates loan account idempotently per applicationId.
 */
@Component
@ConditionalOnProperty(prefix = "loan-management.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LoanEventsConsumer {

  private final ObjectMapper mapper = new ObjectMapper();
  private final LoanManagementService service;

  @KafkaListener(topics = "${loan-management.events.loanOriginationTopic:loan.origination.events}", groupId = "loan-management")
  @Transactional
  public void onLoanOriginationEvent(
      String message,
      @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
      @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
      @Header(name = KafkaHeaders.OFFSET, required = false) Long offset
  ) {
    try {
      JsonNode root = mapper.readTree(message);
      String type = text(root, "type");
      if (!"LoanApproved".equalsIgnoreCase(type)) {
        return;
      }
      String correlationId = text(root, "correlationId");
      JsonNode payload = root.path("payload");

      String applicationId = text(payload, "applicationId");
      Long customerId = longVal(payload, "customerId");
      String loanType = text(payload, "loanType");
      java.math.BigDecimal approvedAmount = decimal(payload, "approvedAmount");
      java.math.BigDecimal interestRate = decimal(payload, "interestRate");
      String decisionBy = text(payload, "decisionBy");

      if (applicationId == null || applicationId.isBlank() || customerId == null || approvedAmount == null || interestRate == null) {
        log.warn("Invalid LoanApproved payload, skipping. raw={}", message);
        return;
      }

      LoanApprovedPayload p = new LoanApprovedPayload(
          applicationId, customerId, loanType, approvedAmount, interestRate, decisionBy
      );
      service.handleLoanApproved(p, correlationId);
      log.info("Handled LoanApproved for applicationId={} (topic={}, key={}, offset={})", applicationId, topic, key, offset);

    } catch (Exception ex) {
      log.error("Failed to process loan origination event (topic={}, key={}, offset={}): {}", topic, key, offset, ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode n = node.path(field);
    return n.isMissingNode() || n.isNull() ? null : n.asText();
  }

  private static java.math.BigDecimal decimal(JsonNode node, String field) {
    JsonNode n = node.path(field);
    if (n == null || n.isMissingNode() || n.isNull()) return null;
    if (n.isNumber()) return n.decimalValue();
    try {
      return new java.math.BigDecimal(n.asText());
    } catch (Exception e) {
      return null;
    }
  }

  private static Long longVal(JsonNode node, String field) {
    JsonNode n = node.path(field);
    if (n == null || n.isMissingNode() || n.isNull()) return null;
    if (n.isNumber()) return n.longValue();
    try {
      return Long.parseLong(n.asText());
    } catch (Exception e) {
      return null;
    }
  }
}
