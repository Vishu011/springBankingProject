package com.omnibank.ledger.config;

import org.springframework.context.annotation.Configuration;

/**
 * Dev placeholder. Spring Security is not on the classpath in the ledger-service.
 * All requests are permitted by default.
 */
@Configuration
public class SecurityConfig {
  // No security filters configured.
}
