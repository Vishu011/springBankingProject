package com.omnibank.loanmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "loan-management")
public class AppProperties {

  private Events events = new Events();
  private Kafka kafka = new Kafka();

  public Events getEvents() {
    return events;
  }

  public void setEvents(Events events) {
    this.events = events;
  }

  public Kafka getKafka() {
    return kafka;
  }

  public void setKafka(Kafka kafka) {
    this.kafka = kafka;
  }

  public static class Events {
    private String loanOriginationTopic = "loan.origination.events";
    private String ledgerTopic = "ledger.events";

    public String getLoanOriginationTopic() {
      return loanOriginationTopic;
    }

    public void setLoanOriginationTopic(String loanOriginationTopic) {
      this.loanOriginationTopic = loanOriginationTopic;
    }

    public String getLedgerTopic() {
      return ledgerTopic;
    }

    public void setLedgerTopic(String ledgerTopic) {
      this.ledgerTopic = ledgerTopic;
    }
  }

  public static class Kafka {
    private boolean enabled = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
