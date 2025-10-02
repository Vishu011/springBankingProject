package com.omnibank.loanorigination.integration;

import com.omnibank.loanorigination.config.AppProperties;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

/**
 * Minimal client for account-management queries.
 */
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
  @Bulkhead(name = "accountManagement")
  public List<AccountDto> listCustomerAccounts(Long customerId, String correlationId) {
    String base = props.getIntegrations().getAccountManagement().getBaseUrl();
    String url = base + "/api/v1/customers/" + customerId + "/accounts";
    AccountDto[] arr = rest.get()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(AccountDto[].class);
    return arr == null ? List.of() : Arrays.asList(arr);
  }

  public static class AccountDto {
    public String accountNumber;
    public Long customerId;
    public String accountType; // SAVINGS, CURRENT, etc.
    public String status;      // ACTIVE, CLOSED, DORMANT
    public java.math.BigDecimal balance;
  }
}
