package com.omnibank.paymentgateway.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibank.paymentgateway.events.EventPublisher;
import com.omnibank.paymentgateway.integration.AccountManagementClient;
import com.omnibank.paymentgateway.integration.BeneficiaryManagementClient;
import com.omnibank.paymentgateway.integration.CustomerProfileClient;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestClientResponseException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @MockBean
  private CustomerProfileClient customerProfileClient;

  @MockBean
  private AccountManagementClient accountManagementClient;

  @MockBean
  private BeneficiaryManagementClient beneficiaryManagementClient;

  @MockBean
  private EventPublisher eventPublisher;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private static HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    h.set("X-Correlation-Id", "test-correlation-id");
    return h;
  }

  @Test
  void internalTransfer_happyPath_returnsProcessing() throws Exception {
    // Arrange mocks
    Mockito.doNothing().when(customerProfileClient).assertCustomerExists(1L, "test-correlation-id");
    Mockito.when(beneficiaryManagementClient.isBeneficiaryActive(1L, "AC222", "test-correlation-id"))
        .thenReturn(true);
    Mockito.when(accountManagementClient.getBalance("AC111", "test-correlation-id"))
        .thenReturn(new BigDecimal("1000.00"));
    // eventPublisher.publish is mocked; no-op

    String body = """
        {
          "customerId": 1,
          "fromAccount": "AC111",
          "toAccount": "AC222",
          "amount": 100.00,
          "currency": "USD"
        }
        """;

    // Act
    ResponseEntity<String> resp = restTemplate.postForEntity(
        url("/api/v1/payments/internal-transfer"),
        new HttpEntity<>(body, headers()),
        String.class
    );

    // Assert
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    JsonNode json = MAPPER.readTree(resp.getBody());
    assertThat(json.hasNonNull("paymentId")).isTrue();
    assertThat(json.get("status").asText()).isEqualTo("PROCESSING");
  }

  @Test
  void internalTransfer_insufficientFunds_returns409() {
    // Arrange mocks
    Mockito.doNothing().when(customerProfileClient).assertCustomerExists(1L, "test-correlation-id");
    Mockito.when(beneficiaryManagementClient.isBeneficiaryActive(1L, "AC222", "test-correlation-id"))
        .thenReturn(true);
    Mockito.when(accountManagementClient.getBalance("AC111", "test-correlation-id"))
        .thenReturn(new BigDecimal("50.00"));

    String body = """
        {
          "customerId": 1,
          "fromAccount": "AC111",
          "toAccount": "AC222",
          "amount": 100.00,
          "currency": "USD"
        }
        """;

    ResponseEntity<String> resp = restTemplate.postForEntity(
        url("/api/v1/payments/internal-transfer"),
        new HttpEntity<>(body, headers()),
        String.class
    );

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(resp.getBody()).contains("Insufficient funds");
  }

  @Test
  void internalTransfer_inactiveBeneficiary_returns409() {
    // Arrange mocks
    Mockito.doNothing().when(customerProfileClient).assertCustomerExists(1L, "test-correlation-id");
    Mockito.when(beneficiaryManagementClient.isBeneficiaryActive(1L, "AC222", "test-correlation-id"))
        .thenReturn(false);
    Mockito.when(accountManagementClient.getBalance("AC111", "test-correlation-id"))
        .thenReturn(new BigDecimal("1000.00"));

    String body = """
        {
          "customerId": 1,
          "fromAccount": "AC111",
          "toAccount": "AC222",
          "amount": 100.00,
          "currency": "USD"
        }
        """;

    ResponseEntity<String> resp = restTemplate.postForEntity(
        url("/api/v1/payments/internal-transfer"),
        new HttpEntity<>(body, headers()),
        String.class
    );

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(resp.getBody()).contains("Beneficiary not active or not found");
  }

  @Test
  void internalTransfer_customerNotFound_returns404() {
    // Arrange mocks: throw a 404 from customer-profile check
    Mockito.doThrow(new RestClientResponseException("Not Found", 404, "Not Found", null, null, null))
        .when(customerProfileClient).assertCustomerExists(1L, "test-correlation-id");

    String body = """
        {
          "customerId": 1,
          "fromAccount": "AC111",
          "toAccount": "AC222",
          "amount": 100.00,
          "currency": "USD"
        }
        """;

    ResponseEntity<String> resp = restTemplate.postForEntity(
        url("/api/v1/payments/internal-transfer"),
        new HttpEntity<>(body, headers()),
        String.class
    );

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(resp.getBody()).contains("Customer not found");
  }

  @Test
  void internalTransfer_fromAccountNotFound_returns404() {
    // Arrange mocks: customer OK, beneficiary active, but account lookup 404
    Mockito.doNothing().when(customerProfileClient).assertCustomerExists(1L, "test-correlation-id");
    Mockito.when(beneficiaryManagementClient.isBeneficiaryActive(1L, "AC222", "test-correlation-id"))
        .thenReturn(true);

    Mockito.when(accountManagementClient.getBalance("AC111", "test-correlation-id"))
        .thenThrow(new RestClientResponseException("Not Found", 404, "Not Found", null, null, null));

    String body = """
        {
          "customerId": 1,
          "fromAccount": "AC111",
          "toAccount": "AC222",
          "amount": 100.00,
          "currency": "USD"
        }
        """;

    ResponseEntity<String> resp = restTemplate.postForEntity(
        url("/api/v1/payments/internal-transfer"),
        new HttpEntity<>(body, headers()),
        String.class
    );

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(resp.getBody()).contains("Account not found");
  }

  @Test
  void internalTransfer_idempotencyKey_returnsSamePaymentIdAndStatus() throws Exception {
    // Arrange mocks
    Mockito.doNothing().when(customerProfileClient).assertCustomerExists(1L, "test-correlation-id");
    Mockito.when(beneficiaryManagementClient.isBeneficiaryActive(1L, "AC222", "test-correlation-id"))
        .thenReturn(true);
    Mockito.when(accountManagementClient.getBalance("AC111", "test-correlation-id"))
        .thenReturn(new BigDecimal("1000.00"));

    String body = """
        {
          "customerId": 1,
          "fromAccount": "AC111",
          "toAccount": "AC222",
          "amount": 100.00,
          "currency": "USD"
        }
        """;

    HttpHeaders h = headers();
    h.set("Idempotency-Key", "idem-1");

    ResponseEntity<String> resp1 = restTemplate.postForEntity(
        url("/api/v1/payments/internal-transfer"),
        new HttpEntity<>(body, h),
        String.class
    );

    ResponseEntity<String> resp2 = restTemplate.postForEntity(
        url("/api/v1/payments/internal-transfer"),
        new HttpEntity<>(body, h),
        String.class
    );

    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

    JsonNode j1 = MAPPER.readTree(resp1.getBody());
    JsonNode j2 = MAPPER.readTree(resp2.getBody());

    assertThat(j1.get("paymentId").asText()).isEqualTo(j2.get("paymentId").asText());
    assertThat(j1.get("status").asText()).isEqualTo(j2.get("status").asText());
    assertThat(j1.get("status").asText()).isEqualTo("PROCESSING");
  }
}
