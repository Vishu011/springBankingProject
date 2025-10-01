package com.omnibank.accountmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AccountManagementApplication {
  public static void main(String[] args) {
    SpringApplication.run(AccountManagementApplication.class, args);
  }
}
