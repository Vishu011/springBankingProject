package com.omnibank.beneficiary.events;

public interface EventPublisher {
  void publish(String topic, String type, Object payload, String correlationId);
}
