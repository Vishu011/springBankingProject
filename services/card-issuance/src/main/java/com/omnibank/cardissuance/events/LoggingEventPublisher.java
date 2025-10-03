package com.omnibank.cardissuance.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-local event publisher that logs outbound events as structured JSON.
 * Switchable to Kafka in future via EventPublisher interface.
 */
@ConditionalOnProperty(prefix = "card-issuance", name = "eventPublisher", havingValue = "logging", matchIfMissing = true)
@Component
public class LoggingEventPublisher implements EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void publish(String topic, String type, Object payload, String correlationId) {
    Map<String, Object> envelope = new HashMap<>();
    envelope.put("topic", topic);
    envelope.put("type", type);
    envelope.put("timestamp", Instant.now().toString());
    envelope.put("correlationId", correlationId);
    envelope.put("payload", payload);
    try {
      log.info("{}", mapper.writeValueAsString(envelope));
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize event payload for type={} topic={}: {}", type, topic, e.getMessage());
    }
  }
}
