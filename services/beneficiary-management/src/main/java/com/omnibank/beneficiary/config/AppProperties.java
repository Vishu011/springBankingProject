package com.omnibank.beneficiary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "beneficiary")
public class AppProperties {

  private Events events = new Events();
  private Otp otp = new Otp();
  private CoolingOff coolingOff = new CoolingOff();
  private String eventPublisher = "logging"; // logging | kafka (future)

  public Events getEvents() {
    return events;
  }

  public void setEvents(Events events) {
    this.events = events;
  }

  public Otp getOtp() {
    return otp;
  }

  public void setOtp(Otp otp) {
    this.otp = otp;
  }

  public CoolingOff getCoolingOff() {
    return coolingOff;
  }

  public void setCoolingOff(CoolingOff coolingOff) {
    this.coolingOff = coolingOff;
  }

  public String getEventPublisher() {
    return eventPublisher;
  }

  public void setEventPublisher(String eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public static class Events {
    private String topic = "beneficiary.events";
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
  }

  public static class Otp {
    private int ttlSeconds = 300;
    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
  }

  public static class CoolingOff {
    private int hours = 24;
    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }
  }
}
