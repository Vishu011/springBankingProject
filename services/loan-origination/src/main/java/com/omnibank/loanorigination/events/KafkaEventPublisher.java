package com.omnibank.loanorigination.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-based event publisher for loan-origination.
 * Enabled when loan-origination.eventPublisher=kafka (see application-kafka.yml).
 * Serializes an event envelope as JSON and publishes to the configured topic.
 */
@ConditionalOnProperty(prefix = "loan-origination", name = "eventPublisher", havingValue = "kafka")
@Component
public class KafkaEventPublisher implements EventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper;

  public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    this.mapper = new ObjectMapper();
  }

  @Override
  public void publish(String topic, String type, Object payload, String correlationId) {
    Map<String, Object> envelope = new HashMap<>();
    envelope.put("topic", topic);
    envelope.put("type", type);
    envelope.put("timestamp", Instant.now().toString());
    if (correlationId != null && !correlationId.isBlank()) {
      envelope.put("correlationId", correlationId);
    }
    envelope.put("payload", payload);

    try {
      String json = mapper.writeValueAsString(envelope);
      String key = (correlationId != null && !correlationId.isBlank()) ? correlationId : type;
      ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json);
      kafkaTemplate.send(record);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize event payload for type=" + type + " topic=" + topic, e);
    }
  }
}
