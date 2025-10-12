package com.selfservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.selfservice.proxy")
@EntityScan(basePackages = "com.selfservice.model")
@EnableJpaRepositories(basePackages = "com.selfservice.dao")
public class SelfServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelfServiceApplication.class, args);
    }
}
