package com.omnibank.customerprofile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerProfileApplication {
  public static void main(String[] args) {
    SpringApplication.run(CustomerProfileApplication.class, args);
  }
}
