package com.omnibank.paymentgateway.integration;

import com.omnibank.paymentgateway.config.AppProperties;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Minimal client to validate a beneficiary is present and active for a given customer.
 */
@Component
public class BeneficiaryManagementClient {

  private final AppProperties props;
  private final RestClient rest;

  public BeneficiaryManagementClient(AppProperties props, RestClient.Builder restBuilder) {
    this.props = props;
    this.rest = restBuilder.build();
  }

  /**
   * Returns true if the beneficiary exists for the owning customer and is ACTIVE (not in cooling-off or blocked).
   * Strategy: call /api/v1/customers/{customerId}/beneficiaries and verify a record matches the toAccount with status ACTIVE.
   * Note: In a future iteration, switch to a specific endpoint if available.
   */
  public boolean isBeneficiaryActive(Long customerId, String toAccount, String correlationId) {
    String base = props.getIntegrations().getBeneficiaryManagement().getBaseUrl();
    String url = base + "/api/v1/customers/" + customerId + "/beneficiaries";
    BeneficiaryDto[] arr = rest.get()
        .uri(URI.create(url))
        .header("X-Correlation-Id", correlationId != null ? correlationId : "")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(BeneficiaryDto[].class);
    if (arr == null) return false;
    return Arrays.stream(arr)
        .filter(Objects::nonNull)
        .anyMatch(b -> "ACTIVE".equalsIgnoreCase(b.status) && toAccount.equalsIgnoreCase(b.accountNumber));
  }

  // Minimal DTO to deserialize list results from beneficiary-management
  public static class BeneficiaryDto {
    public Long id;
    public Long owningCustomerId;
    public String nickname;
    public String accountNumber;
    public String bankCode;
    public String status;
  }
}
