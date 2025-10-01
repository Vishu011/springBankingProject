package com.omnibank.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OnboardingApplication {
  public static void main(String[] args) {
    SpringApplication.run(OnboardingApplication.class, args);
  }
}
