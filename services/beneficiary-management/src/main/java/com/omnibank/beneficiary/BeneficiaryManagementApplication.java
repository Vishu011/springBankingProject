package com.omnibank.beneficiary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BeneficiaryManagementApplication {
  public static void main(String[] args) {
    SpringApplication.run(BeneficiaryManagementApplication.class, args);
  }
}
