package com.omnibank.cardmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "card-management")
public class AppProperties {

  private String eventPublisher = "logging"; // logging | kafka
  private Events events = new Events();
  // Secure-profile knobs
  private boolean requireBlockReason = false;

  public String getEventPublisher() {
    return eventPublisher;
  }

  public void setEventPublisher(String eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public Events getEvents() {
    return events;
  }

  public void setEvents(Events events) {
    this.events = events;
  }

  public boolean isRequireBlockReason() {
    return requireBlockReason;
  }

  public void setRequireBlockReason(boolean requireBlockReason) {
    this.requireBlockReason = requireBlockReason;
  }

  public static class Events {
    private String topic = "card.management.events";
    private String issuanceTopic = "card.issuance.events";

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }

    public String getIssuanceTopic() {
      return issuanceTopic;
    }

    public void setIssuanceTopic(String issuanceTopic) {
      this.issuanceTopic = issuanceTopic;
    }
  }
}
