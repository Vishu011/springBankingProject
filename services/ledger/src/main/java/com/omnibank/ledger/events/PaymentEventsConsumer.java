package com.omnibank.ledger.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibank.ledger.application.LedgerService;
import com.omnibank.ledger.application.LedgerService.Entry;
import com.omnibank.ledger.repository.ProcessedPaymentRepository;
import com.omnibank.ledger.domain.ProcessedPayment;
import io.micrometer.common.util.StringUtils;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for payment.events PaymentApprovedForProcessing.
 * When enabled, consumes payment approvals and posts the double-entry into the ledger,
 * then LedgerService will publish TransactionPosted (logging or kafka as configured).
 *
 * Idempotency:
 *   Uses a ProcessedPayment marker table keyed by paymentUuid to avoid double-posting.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PaymentEventsConsumer {

  private final ObjectMapper mapper = new ObjectMapper();
  private final LedgerService ledgerService;
  private final ProcessedPaymentRepository processedRepo;

  @KafkaListener(topics = "${ledger.events.paymentTopic:payment.events}", groupId = "ledger-service")
  @Transactional
  public void onPaymentEvent(
      String message,
      @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
      @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
      @Header(name = KafkaHeaders.OFFSET, required = false) Long offset
  ) {
    try {
      JsonNode root = mapper.readTree(message);
      String type = text(root, "type");
      if (!"PaymentApprovedForProcessing".equalsIgnoreCase(type)) {
        return;
      }
      String correlationId = text(root, "correlationId"); // optional
      JsonNode payload = root.path("payload");

      String paymentUuid = text(payload, "paymentUuid");
      String fromAccount = text(payload, "fromAccount");
      String toAccount = text(payload, "toAccount");
      BigDecimal amount = decimal(payload, "amount");

      if (StringUtils.isBlank(paymentUuid) || StringUtils.isBlank(fromAccount) || StringUtils.isBlank(toAccount) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        log.warn("Invalid PaymentApprovedForProcessing payload, skipping. raw={}", message);
        return;
      }

      if (processedRepo.existsByPaymentUuid(paymentUuid)) {
        log.debug("Payment already processed, paymentUuid={}, skipping (topic={}, key={}, offset={})", paymentUuid, topic, key, offset);
        return;
      }

      // Build double entry: debit fromAccount, credit toAccount
      List<Entry> entries = List.of(
          new Entry(fromAccount, amount, 'D'),
          new Entry(toAccount, amount, 'C')
      );

      var postRes = ledgerService.postTransaction("TRANSFER", entries, correlationId);
      processedRepo.save(ProcessedPayment.builder().paymentUuid(paymentUuid).build());

      log.info("Processed payment event -> ledger posted txId={} paymentUuid={} (topic={}, key={}, offset={})",
          postRes.getTransactionId(), paymentUuid, topic, key, offset);

    } catch (Exception ex) {
      log.error("Failed to process payment event (topic={}, key={}, offset={}): {}", topic, key, offset, ex.getMessage(), ex);
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
