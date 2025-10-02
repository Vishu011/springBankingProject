package com.omnibank.loanorigination.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "loan-origination")
public class AppProperties {

  private String eventPublisher = "logging"; // logging | kafka (future)
  private Events events = new Events();
  private Integrations integrations = new Integrations();

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

  public Integrations getIntegrations() {
    return integrations;
  }

  public void setIntegrations(Integrations integrations) {
    this.integrations = integrations;
  }

  public static class Events {
    private String topic = "loan.origination.events";

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }
  }

  public static class Integrations {
    // For dev-local we avoid hard dependencies; placeholders for later use
    private ServiceRef paymentGateway = new ServiceRef("http://localhost:8105");

    public ServiceRef getPaymentGateway() {
      return paymentGateway;
    }

    public void setPaymentGateway(ServiceRef paymentGateway) {
      this.paymentGateway = paymentGateway;
    }

    public static class ServiceRef {
      private String baseUrl;

      public ServiceRef() {}

      public ServiceRef(String baseUrl) {
        this.baseUrl = baseUrl;
      }

      public String getBaseUrl() {
        return baseUrl;
      }

      public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
      }
    }
  }
}
