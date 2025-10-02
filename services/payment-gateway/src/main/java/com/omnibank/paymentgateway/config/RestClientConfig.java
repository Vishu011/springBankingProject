package com.omnibank.paymentgateway.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Central RestClient configuration for timeouts and common tuning.
 * Keeps dev-local fast-fail semantics and avoids long hangs on downstream issues.
 */
@Configuration
public class RestClientConfig {

  @Bean
  public RestClient.Builder restClientBuilder() {
    // Simple HTTP request factory with connect/read timeouts
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    // setConnectTimeout/readTimeout use int milliseconds on SimpleClientHttpRequestFactory
    rf.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
    rf.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

    return RestClient.builder().requestFactory(rf);
  }
}
