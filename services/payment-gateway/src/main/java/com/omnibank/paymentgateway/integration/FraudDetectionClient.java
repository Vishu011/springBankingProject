package com.omnibank.paymentgateway.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnibank.paymentgateway.config.AppProperties;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Synchronous fraud decision client.
 * In dev, if the endpoint is unreachable or returns non-2xx, caller should treat as ALLOW.
 */
@Component
public class FraudDetectionClient {

  private final AppProperties props;
  private final RestClient rest = RestClient.create();

  public FraudDetectionClient(AppProperties props) {
    this.props = props;
  }

  public Decision getDecision(FraudRequest req, String correlationId) {
    String base = props.getIntegrations().getFraudDetection().getBaseUrl();
    // Placeholder internal dev path; replace when real fraud service is available
    String url = base + "/api/v1/internal/dev/score";

    try {
      Decision d = rest.post()
          .uri(URI.create(url))
          .header("X-Correlation-Id", correlationId != null ? correlationId : "")
          .contentType(MediaType.APPLICATION_JSON)
          .body(req)
          .retrieve()
          .body(Decision.class);
      if (d == null) {
        return Decision.allow();
      }
      // Normalize action casing
      String action = d.action != null ? d.action.toUpperCase() : "ALLOW";
      BigDecimal score = d.score != null ? d.score : BigDecimal.ZERO;
      return new Decision(action, score);
    } catch (Exception ex) {
      // Network or non-2xx; default to ALLOW in dev
      return Decision.allow();
    }
  }

  // Request DTO sent to fraud engine (minimal placeholder)
  public static class FraudRequest {
    @JsonProperty public Long customerId;
    @JsonProperty public String fromAccount;
    @JsonProperty public String toAccount;
    @JsonProperty public BigDecimal amount;
    @JsonProperty public String currency;

    public static FraudRequest of(Long customerId, String fromAccount, String toAccount, BigDecimal amount, String currency) {
      FraudRequest r = new FraudRequest();
      r.customerId = customerId;
      r.fromAccount = fromAccount;
      r.toAccount = toAccount;
      r.amount = amount;
      r.currency = currency;
      return r;
    }
  }

  // Response DTO from fraud engine
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Decision {
    @JsonProperty public String action;   // ALLOW | CHALLENGE | BLOCK
    @JsonProperty public BigDecimal score;

    public Decision() {}

    public Decision(String action, BigDecimal score) {
      this.action = action;
      this.score = score;
    }

    public static Decision allow() {
      return new Decision("ALLOW", BigDecimal.ZERO);
    }
  }
}
