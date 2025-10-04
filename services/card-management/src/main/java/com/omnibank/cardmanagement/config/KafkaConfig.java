package com.omnibank.cardmanagement.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Enables Kafka listener processing when explicitly turned on.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "card-management.kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {
}
