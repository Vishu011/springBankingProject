package com.omnibank.customerprofile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "customerprofile")
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
    private String topic = "customer.profile.events";

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }
  }
}
