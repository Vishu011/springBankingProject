package com.omnibank.loanorigination.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Central RestClient configuration for timeouts and common tuning.
 * Keeps dev-local fast-fail semantics and avoids long hangs on downstream issues.
 */
@Configuration
public class RestClientConfig {

  @Bean
  public RestClient.Builder restClientBuilder() {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
    rf.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
    return RestClient.builder().requestFactory(rf);
  }
}
