package com.omnibank.accountmanagement.events;

public interface EventPublisher {
  void publish(String topic, String type, Object payload, String correlationId);
}
