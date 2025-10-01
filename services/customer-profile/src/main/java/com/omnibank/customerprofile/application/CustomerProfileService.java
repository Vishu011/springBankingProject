package com.omnibank.customerprofile.application;

import com.omnibank.customerprofile.api.dto.AgentApproveUpdateRequestDto;
import com.omnibank.customerprofile.api.dto.AddressUpdateRequestDto;
import com.omnibank.customerprofile.api.dto.OnboardingApprovedRequest;
import com.omnibank.customerprofile.config.AppProperties;
import com.omnibank.customerprofile.domain.Address;
import com.omnibank.customerprofile.domain.Customer;
import com.omnibank.customerprofile.domain.CustomerStatus;
import com.omnibank.customerprofile.domain.UpdateRequest;
import com.omnibank.customerprofile.events.EventPublisher;
import com.omnibank.customerprofile.events.EventTypes;
import com.omnibank.customerprofile.repository.AddressRepository;
import com.omnibank.customerprofile.repository.CustomerRepository;
import com.omnibank.customerprofile.repository.UpdateRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

  private final CustomerRepository customerRepository;
  private final AddressRepository addressRepository;
  private final UpdateRequestRepository updateRequestRepository;
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  @Transactional
  public Long handleOnboardingApproved(OnboardingApprovedRequest req, String correlationId) {
    // Generate CIF ID (20 chars max) - CIF + 16 hex chars from UUID (without dashes)
    String cifId = "CIF" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

    Customer customer = Customer.builder()
        .cifId(cifId)
        .status(CustomerStatus.ACTIVE)
        .firstName(req.getFirstName())
        .lastName(req.getLastName())
        .email(req.getEmail())
        .mobile(req.getMobileNumber())
        .build();
    customerRepository.save(customer);

    // Optional: seed a placeholder ACTIVE address record (can be updated later via update request)
    Address placeholder = Address.builder()
        .customer(customer)
        .addressLine1("PENDING_ADDRESS_UPDATE")
        .city("PENDING")
        .status("ACTIVE")
        .build();
    addressRepository.save(placeholder);

    // Publish profile created event
    publish(EventTypes.CUSTOMER_PROFILE_CREATED,
        new EventPayloads.CustomerProfileCreated(customer.getId(), customer.getCifId(),
            customer.getFirstName(), customer.getLastName(), customer.getEmail(), customer.getMobile()),
        correlationId);

    return customer.getId();
  }

  @Transactional(readOnly = true)
  public Customer getCustomer(Long customerId) {
    return customerRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
  }

  @Transactional
  public Long createAddressUpdateRequest(Long customerId, AddressUpdateRequestDto dto, String correlationId) {
    Customer customer = getCustomer(customerId);

    String submittedJson = """
        {"addressLine1":"%s","city":"%s"}
        """.formatted(safe(dto.getAddressLine1()), safe(dto.getCity()));

    UpdateRequest req = UpdateRequest.builder()
        .customer(customer)
        .requestType("ADDRESS_UPDATE")
        .status("PENDING_VERIFICATION")
        .submittedData(submittedJson)
        .build();
    updateRequestRepository.save(req);
    return req.getId();
  }

  @Transactional
  public void approveUpdateRequest(AgentApproveUpdateRequestDto dto, String correlationId) {
    UpdateRequest req = updateRequestRepository.findById(dto.getRequestId())
        .orElseThrow(() -> new ResourceNotFoundException("UpdateRequest not found: " + dto.getRequestId()));
    if (!"ADDRESS_UPDATE".equals(req.getRequestType())) {
      throw new IllegalArgumentException("Only ADDRESS_UPDATE requests are supported");
    }

    Customer customer = req.getCustomer();

    // Deactivate existing addresses
    List<Address> existing = addressRepository.findByCustomerOrderByCreatedAtDesc(customer);
    for (Address a : existing) {
      if ("ACTIVE".equalsIgnoreCase(a.getStatus())) {
        a.setStatus("INACTIVE");
      }
    }
    addressRepository.saveAll(existing);

    // Parse submittedData (very simple; in real system use a JSON parser)
    String addressLine1 = extractJsonField(req.getSubmittedData(), "addressLine1");
    String city = extractJsonField(req.getSubmittedData(), "city");

    // Create new ACTIVE address
    Address newAddress = Address.builder()
        .customer(customer)
        .addressLine1(addressLine1)
        .city(city)
        .status("ACTIVE")
        .build();
    addressRepository.save(newAddress);

    // Mark request approved
    req.setStatus("APPROVED");
    req.setApprovedBy(dto.getApprovedBy());
    req.setApprovedTs(Instant.now());
    updateRequestRepository.save(req);

    // Publish address updated
    publish(EventTypes.CUSTOMER_ADDRESS_UPDATED,
        new EventPayloads.CustomerAddressUpdated(customer.getId(), newAddress.getId(), newAddress.getAddressLine1(), newAddress.getCity()),
        correlationId);
  }

  private void publish(String type, Object payload, String correlationId) {
    eventPublisher.publish(props.getEvents().getTopic(), type, payload, correlationId);
  }

  private static String safe(String v) {
    return v == null ? "" : v.replace("\"", "\\\"");
  }

  private static String extractJsonField(String json, String key) {
    if (json == null) return "";
    String pattern = "\"%s\":\"".formatted(key);
    int start = json.indexOf(pattern);
    if (start < 0) return "";
    start += pattern.length();
    int end = json.indexOf("\"", start);
    if (end < 0) return "";
    return json.substring(start, end);
  }

  public static class EventPayloads {
    public record CustomerProfileCreated(Long customerId, String cifId, String firstName, String lastName, String email, String mobile) {}
    public record CustomerAddressUpdated(Long customerId, Long addressId, String addressLine1, String city) {}
  }

  // 404 semantics
  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
  }
}
