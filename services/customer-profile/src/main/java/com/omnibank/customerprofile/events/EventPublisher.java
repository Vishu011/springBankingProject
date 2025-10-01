package com.omnibank.customerprofile.events;

public interface EventPublisher {
  void publish(String topic, String type, Object payload, String correlationId);
}
