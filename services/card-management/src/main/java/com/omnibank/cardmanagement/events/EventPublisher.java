package com.omnibank.cardmanagement.events;

public interface EventPublisher {
  void publish(String topic, String type, Object payload, String correlationId);
}
