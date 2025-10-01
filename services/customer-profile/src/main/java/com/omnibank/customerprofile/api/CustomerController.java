package com.omnibank.customerprofile.api;

import com.omnibank.customerprofile.api.dto.AgentApproveUpdateRequestDto;
import com.omnibank.customerprofile.api.dto.AddressUpdateRequestDto;
import com.omnibank.customerprofile.api.dto.OnboardingApprovedRequest;
import com.omnibank.customerprofile.application.CustomerProfileService;
import com.omnibank.customerprofile.domain.Customer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final CustomerProfileService service;

  @PostMapping("/internal/dev/onboarding-approved")
  public ResponseEntity<Long> devOnboardingApproved(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody OnboardingApprovedRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    Long customerId = service.handleOnboardingApproved(request, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(customerId);
  }

  @GetMapping("/customers/{customerId}")
  public ResponseEntity<Customer> getCustomer(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("customerId") Long customerId
  ) {
    String cid = ensureCorrelationId(correlationId);
    Customer customer = service.getCustomer(customerId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(customer);
  }

  @PostMapping("/customers/{customerId}/address-update-requests")
  public ResponseEntity<Long> createAddressUpdateRequest(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("customerId") Long customerId,
      @Valid @RequestBody AddressUpdateRequestDto request
  ) {
    String cid = ensureCorrelationId(correlationId);
    Long requestId = service.createAddressUpdateRequest(customerId, request, cid);
    return ResponseEntity.accepted()
        .header(HDR_CORRELATION_ID, cid)
        .body(requestId);
  }

  @PostMapping("/customers/agent/approve-update-request")
  public ResponseEntity<Void> approveUpdateRequest(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody AgentApproveUpdateRequestDto request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.approveUpdateRequest(request, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .build();
  }

  private String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
