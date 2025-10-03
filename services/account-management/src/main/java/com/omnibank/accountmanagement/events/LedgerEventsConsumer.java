package com.omnibank.accountmanagement.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibank.accountmanagement.domain.Account;
import com.omnibank.accountmanagement.domain.LedgerAppliedEvent;
import com.omnibank.accountmanagement.repository.AccountRepository;
import com.omnibank.accountmanagement.repository.LedgerAppliedEventRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for ledger.events TransactionPosted.
 * Disabled by default; enable by setting account.kafka.enabled=true and configuring spring.kafka.*.
 * Expects envelope JSON published by services using the EventPublisher pattern:
 * {
 *   "topic":"ledger.events",
 *   "type":"TransactionPosted",
 *   "timestamp":"...",
 *   "correlationId":"...",
 *   "payload": {
 *     "transactionId":"...",
 *     "entries":[{"account":"...","amount":123.45,"direction":"D" | "C"}, ...]
 *   }
 * }
 */
@Component
@ConditionalOnProperty(prefix = "account.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LedgerEventsConsumer {

  private final ObjectMapper mapper = new ObjectMapper();

  private final AccountRepository accountRepository;
  private final LedgerAppliedEventRepository appliedRepo;

  @KafkaListener(topics = "${account.events.ledgerTopic:ledger.events}", groupId = "account-management")
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

      if (appliedRepo.existsByTransactionId(txId)) {
        log.debug("TransactionPosted already applied, txId={}, skipping", txId);
        return;
      }

      JsonNode entries = payload.path("entries");
      if (entries == null || !entries.isArray() || entries.size() == 0) {
        log.warn("TransactionPosted has no entries, txId={}, skipping", txId);
        return;
      }

      for (JsonNode e : entries) {
        String accountNo = text(e, "account");
        BigDecimal amount = decimal(e, "amount");
        char direction = dir(e, "direction");
        if (accountNo == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
          throw new IllegalArgumentException("Invalid entry in TransactionPosted: " + e);
        }

        var accountOpt = accountRepository.findById(accountNo);
        if (accountOpt.isEmpty()) {
          log.debug("Account not found for ledger entry account={}, skipping", accountNo);
          continue;
        }
        Account account = accountOpt.get();
        BigDecimal delta = (direction == 'D') ? amount.negate() : amount;
        account.setBalance(account.getBalance().add(delta));
        accountRepository.save(account);
      }

      appliedRepo.save(
          LedgerAppliedEvent.builder()
              .transactionId(txId)
              .build()
      );

      log.info("Applied TransactionPosted txId={} entries={} (topic={}, key={}, offset={})",
          txId, payload.path("entries").size(), topic, key, offset);

    } catch (Exception ex) {
      // Let the record be retried as per Kafka listener container config (default retries/backoff).
      log.error("Failed to process ledger event (topic={}, key={}, offset={}): {}",
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

  private static char dir(JsonNode node, String field) {
    String v = text(node, field);
    return (v != null && !v.isBlank()) ? Character.toUpperCase(v.charAt(0)) : ' ';
  }
}
