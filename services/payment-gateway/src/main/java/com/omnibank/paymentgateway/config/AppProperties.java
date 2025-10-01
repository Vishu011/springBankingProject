package com.omnibank.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment-gateway")
public class AppProperties {

  private String eventPublisher = "logging"; // logging | kafka
  private Events events = new Events();
  private Integrations integrations = new Integrations();
  private boolean devSyncPosting = true;

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

  public boolean isDevSyncPosting() {
    return devSyncPosting;
  }

  public void setDevSyncPosting(boolean devSyncPosting) {
    this.devSyncPosting = devSyncPosting;
  }

  public static class Events {
    private String topic = "payment.events";

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }
  }

  public static class Integrations {
    private ServiceRef customerProfile = new ServiceRef("http://localhost:8102");
    private ServiceRef accountManagement = new ServiceRef("http://localhost:8103");
    private ServiceRef beneficiaryManagement = new ServiceRef("http://localhost:8104");
    private ServiceRef fraudDetection = new ServiceRef("http://localhost:8110"); // placeholder
    private ServiceRef ledger = new ServiceRef("http://localhost:8106"); // dev-first

    public ServiceRef getCustomerProfile() {
      return customerProfile;
    }

    public void setCustomerProfile(ServiceRef customerProfile) {
      this.customerProfile = customerProfile;
    }

    public ServiceRef getAccountManagement() {
      return accountManagement;
    }

    public void setAccountManagement(ServiceRef accountManagement) {
      this.accountManagement = accountManagement;
    }

    public ServiceRef getBeneficiaryManagement() {
      return beneficiaryManagement;
    }

    public void setBeneficiaryManagement(ServiceRef beneficiaryManagement) {
      this.beneficiaryManagement = beneficiaryManagement;
    }

    public ServiceRef getFraudDetection() {
      return fraudDetection;
    }

    public void setFraudDetection(ServiceRef fraudDetection) {
      this.fraudDetection = fraudDetection;
    }

    public ServiceRef getLedger() {
      return ledger;
    }

    public void setLedger(ServiceRef ledger) {
      this.ledger = ledger;
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
