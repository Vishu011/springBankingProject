package com.omnibank.paymentgateway.integration;

import com.omnibank.paymentgateway.config.AppProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class AccountManagementClient {

  private final AppProperties props;
  private final RestClient rest;

  public AccountManagementClient(AppProperties props, RestClient.Builder restBuilder) {
    this.props = props;
    this.rest = restBuilder.build();
  }

  @CircuitBreaker(name = "accountManagement")
  @Retry(name = "accountManagement")
  public BigDecimal getBalance(String accountNumber, String correlationId) {
    String base = props.getIntegrations().getAccountManagement().getBaseUrl();
    String url = base + "/api/v1/accounts/" + accountNumber + "/balance";
    return rest.get()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(BigDecimal.class);
  }

  /**
   * Dev-only balance adjustment endpoint in account-management.
   * Positive amount = credit; negative amount = debit.
   */
  @CircuitBreaker(name = "accountManagement")
  @Retry(name = "accountManagement")
  public BigDecimal adjustBalanceDev(String accountNumber, BigDecimal amount, String correlationId) {
    String base = props.getIntegrations().getAccountManagement().getBaseUrl();
    String url = base + "/api/v1/internal/dev/accounts/" + accountNumber + "/adjust-balance";
    Map<String, Object> body = new HashMap<>();
    body.put("amount", amount);
    return rest.post()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(BigDecimal.class);
  }
}
