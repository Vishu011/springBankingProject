package com.omnibank.cardissuance.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka-backed EventPublisher. Enabled when card-issuance.eventPublisher=kafka.
 * Publishes a consistent envelope:
 * {
 *   "topic": "...",
 *   "type": "...",
 *   "timestamp": "...",
 *   "correlationId": "...",
 *   "payload": {...}
 * }
 */
@ConditionalOnProperty(prefix = "card-issuance", name = "eventPublisher", havingValue = "kafka")
@Component
public class KafkaEventPublisher implements EventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper = new ObjectMapper();

  public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public void publish(String topic, String type, Object payload, String correlationId) {
    Map<String, Object> envelope = new HashMap<>();
    envelope.put("topic", topic);
    envelope.put("type", type);
    envelope.put("timestamp", Instant.now().toString());
    envelope.put("correlationId", correlationId);
    envelope.put("payload", payload);

    try {
      String json = mapper.writeValueAsString(envelope);
      // Use type as key for partitioning consistency; correlationId could also be used
      kafkaTemplate.send(topic, type, json);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize event payload for type=" + type + " topic=" + topic + ": " + e.getMessage(), e);
    }
  }
}
