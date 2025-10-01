package com.omnibank.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "onboarding")
public class AppProperties {

  private Events events = new Events();
  private String eventPublisher = "logging"; // logging | kafka (future)
  private Integrations integrations = new Integrations();

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

  public Integrations getIntegrations() {
    return integrations;
  }

  public void setIntegrations(Integrations integrations) {
    this.integrations = integrations;
  }

  public static class Integrations {
    private CustomerProfile customerProfile = new CustomerProfile();

    public CustomerProfile getCustomerProfile() {
      return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
      this.customerProfile = customerProfile;
    }

    public static class CustomerProfile {
      private String baseUrl = "http://localhost:8102";

      public String getBaseUrl() {
        return baseUrl;
      }

      public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
      }
    }
  }

  public static class Events {
    private String topic = "onboarding.events";

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }
  }
}
