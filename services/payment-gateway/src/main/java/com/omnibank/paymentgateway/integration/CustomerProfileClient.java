package com.omnibank.paymentgateway.integration;

import com.omnibank.paymentgateway.config.AppProperties;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Minimal client used to validate a customer exists and is retrievable.
 */
@Component
public class CustomerProfileClient {

  private final AppProperties props;
  private final RestClient rest = RestClient.create();

  public CustomerProfileClient(AppProperties props) {
    this.props = props;
  }

  public void assertCustomerExists(Long customerId, String correlationId) {
    String base = props.getIntegrations().getCustomerProfile().getBaseUrl();
    String url = base + "/api/v1/customers/" + customerId;
    // We don't need the body here, just ensure 200 OK
    rest.get()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toBodilessEntity();
  }
}
