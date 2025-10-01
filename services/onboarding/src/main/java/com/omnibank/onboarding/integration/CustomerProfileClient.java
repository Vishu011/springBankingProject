package com.omnibank.onboarding.integration;

import com.omnibank.onboarding.config.AppProperties;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CustomerProfileClient {

  private final AppProperties props;
  private final RestClient rest = RestClient.create();

  public void sendOnboardingApproved(String firstName,
                                     String lastName,
                                     String email,
                                     String mobileNumber,
                                     String applicationId,
                                     String correlationId) {
    String base = props.getIntegrations().getCustomerProfile().getBaseUrl();
    String url = base + "/api/v1/internal/dev/onboarding-approved";

    Map<String, Object> body = new HashMap<>();
    body.put("firstName", firstName);
    body.put("lastName", lastName);
    body.put("email", email);
    body.put("mobileNumber", mobileNumber);
    body.put("applicationId", applicationId);

    rest.post()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }
}
