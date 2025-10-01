package com.omnibank.accountmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "account")
public class AppProperties {

  private Events events = new Events();
  private String eventPublisher = "logging"; // logging | kafka (future)

  public Events getEvents() {
    return events;
  }

  public void setEvents(Events events) {
    this.events = events;
  }

  public String getEventPublisher() {
    return eventPublisher;
  }

  public void setEventPublisher(String eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public static class Events {
    private String topic = "account.events";

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }
  }
}
