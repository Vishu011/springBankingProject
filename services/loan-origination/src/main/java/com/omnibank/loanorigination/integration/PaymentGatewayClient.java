package com.omnibank.loanorigination.integration;

import com.omnibank.loanorigination.config.AppProperties;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

@Component
public class PaymentGatewayClient {

  private final AppProperties props;
  private final RestClient rest;

  public PaymentGatewayClient(AppProperties props, RestClient.Builder restBuilder) {
    this.props = props;
    this.rest = restBuilder.build();
  }

  @Bulkhead(name = "paymentGateway")
  public InitiateResponse initiateInternalTransfer(
      Long customerId,
      String fromAccount,
      String toAccount,
      BigDecimal amount,
      String currency,
      String correlationId,
      String idempotencyKey
  ) {
    String base = props.getIntegrations().getPaymentGateway().getBaseUrl();
    String url = base + "/api/v1/payments/internal-transfer";
    Body body = new Body();
    body.customerId = customerId;
    body.fromAccount = fromAccount;
    body.toAccount = toAccount;
    body.amount = amount;
    body.currency = currency;

    return rest.post()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .header("Idempotency-Key", idempotencyKey != null ? idempotencyKey : "")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(InitiateResponse.class);
  }

  public static class Body {
    public Long customerId;
    public String fromAccount;
    public String toAccount;
    public BigDecimal amount;
    public String currency;
  }

  public static class InitiateResponse {
    public String paymentId;
    public String status;
  }
}
