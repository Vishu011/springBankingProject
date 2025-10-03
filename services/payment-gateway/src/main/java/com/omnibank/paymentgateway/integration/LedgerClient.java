package com.omnibank.paymentgateway.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnibank.paymentgateway.config.AppProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

/**
 * Dev-first client for the ledger-service internal posting endpoint.
 * POST {ledger.baseUrl}/api/v1/internal/ledger/transactions
 */
@Component
public class LedgerClient {

  private final AppProperties props;
  private final RestClient rest;

  public LedgerClient(AppProperties props, RestClient.Builder restBuilder) {
    this.props = props;
    this.rest = restBuilder.build();
  }

  @Bulkhead(name = "ledger")
  public PostResult postTransfer(String fromAccount,
                                 String toAccount,
                                 BigDecimal amount,
                                 String currency,
                                 String correlationId) {
    // currency is ignored by the ledger for now (stored only as amounts)
    String base = props.getIntegrations().getLedger().getBaseUrl();
    String url = base + "/api/v1/internal/ledger/transactions";

    PostTransactionRequest req = new PostTransactionRequest();
    req.transactionType = "TRANSFER";
    req.entries = List.of(
        new PostEntry(fromAccount, amount, 'D'),
        new PostEntry(toAccount, amount, 'C')
    );

    return rest.post()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .contentType(MediaType.APPLICATION_JSON)
        .body(req)
        .retrieve()
        .body(PostResult.class);
  }

  // Request DTOs
  public static class PostTransactionRequest {
    @JsonProperty public String transactionType;
    @JsonProperty public List<PostEntry> entries;
  }

  public static class PostEntry {
    @JsonProperty public String accountNumber;
    @JsonProperty public BigDecimal amount;
    @JsonProperty public Character direction;

    public PostEntry() {}

    public PostEntry(String accountNumber, BigDecimal amount, Character direction) {
      this.accountNumber = accountNumber;
      this.amount = amount;
      this.direction = direction;
    }
  }

  // Response DTO from ledger
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PostResult {
    @JsonProperty public String transactionId;
    @JsonProperty public String status;
  }
}
