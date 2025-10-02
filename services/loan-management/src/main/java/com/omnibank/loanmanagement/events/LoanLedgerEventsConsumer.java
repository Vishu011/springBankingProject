package com.omnibank.loanmanagement.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibank.loanmanagement.application.LoanManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Consumes ledger.events TransactionPosted and applies EMIs idempotently
 * when an entry maps to a loan account number.
 *
 * Envelope shape:
 * {
 *   "topic":"ledger.events",
 *   "type":"TransactionPosted",
 *   "timestamp":"...",
 *   "correlationId":"...",
 *   "payload": {
 *     "transactionId":"...",
 *     "entries":[{"account":"...","amount":123.45,"direction":"D"|"C"}, ...]
 *   }
 * }
 *
 * MVP rule: treat any entry whose account equals a LoanAccount.loanAccountNumber as an EMI payment.
 * Amount is applied as paid amount (direction ignored for MVP simplification).
 */
@Component
@ConditionalOnProperty(prefix = "loan-management.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LoanLedgerEventsConsumer {

  private final ObjectMapper mapper = new ObjectMapper();
  private final LoanManagementService service;

  @KafkaListener(topics = "${loan-management.events.ledgerTopic:ledger.events}", groupId = "loan-management")
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
      String txId = text(payload, "transactionId");
      if (txId == null || txId.isBlank()) {
        log.warn("TransactionPosted missing transactionId, skipping. raw={}", message);
        return;
      }

      JsonNode entries = payload.path("entries");
      if (entries == null || !entries.isArray() || entries.size() == 0) {
        log.warn("TransactionPosted has no entries, txId={}, skipping", txId);
        return;
      }

      for (JsonNode e : entries) {
        String account = text(e, "account");
        BigDecimal amount = decimal(e, "amount");
        if (account == null || account.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
          continue;
        }

        // Heuristic: loan accounts start with "LN" but we accept any that exist in repo inside service
        try {
          service.applyEmiPayment(txId, account, amount);
        } catch (IllegalArgumentException notLoanAcc) {
          // Not a loan account; ignore
        }
      }

      log.info("Processed TransactionPosted for loan EMI application, txId={} (topic={}, key={}, offset={})",
          txId, topic, key, offset);

    } catch (Exception ex) {
      log.error("Failed to process ledger TransactionPosted for loan management (topic={}, key={}, offset={}): {}",
          topic, key, offset, ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode n = node.path(field);
    return n.isMissingNode() || n.isNull() ? null : n.asText();
  }

  private static BigDecimal decimal(JsonNode node, String field) {
    JsonNode n = node.path(field);
    if (n == null || n.isMissingNode() || n.isNull()) return null;
    if (n.isNumber()) return n.decimalValue();
    try {
      return new BigDecimal(n.asText());
    } catch (Exception e) {
      return null;
    }
  }
}
