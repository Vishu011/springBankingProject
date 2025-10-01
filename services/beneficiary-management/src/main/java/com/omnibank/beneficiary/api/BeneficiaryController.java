package com.omnibank.beneficiary.api;

import com.omnibank.beneficiary.api.dto.CreateBeneficiaryRequest;
import com.omnibank.beneficiary.api.dto.VerifyOtpRequest;
import com.omnibank.beneficiary.api.dto.RiskScoreRequest;
import com.omnibank.beneficiary.application.BeneficiaryManagementService;
import com.omnibank.beneficiary.domain.Beneficiary;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BeneficiaryController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final BeneficiaryManagementService service;

  @PostMapping("/customers/{customerId}/beneficiaries")
  public ResponseEntity<Map<String, Object>> create(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("customerId") Long customerId,
      @Valid @RequestBody CreateBeneficiaryRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    var res = service.createBeneficiary(customerId, request, cid);
    Map<String, Object> body = new HashMap<>();
    body.put("beneficiaryId", res.beneficiaryId());
    body.put("challengeId", res.challengeId());
    body.put("otpDevEcho", res.otpDevEcho()); // dev-only: helpful for manual testing
    return ResponseEntity.accepted()
        .header(HDR_CORRELATION_ID, cid)
        .body(body);
  }

  @PostMapping("/beneficiaries/verify-otp")
  public ResponseEntity<Void> verifyOtp(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody VerifyOtpRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.verifyOtp(request.getOwningCustomerId(), request.getBeneficiaryId(), request.getChallengeId(), request.getCode(), cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .build();
  }

  @GetMapping("/customers/{customerId}/beneficiaries")
  public ResponseEntity<List<Beneficiary>> list(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("customerId") Long customerId
  ) {
    String cid = ensureCorrelationId(correlationId);
    List<Beneficiary> items = service.list(customerId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(items);
  }

  @GetMapping("/beneficiaries/{beneficiaryId}/cooling-off-status")
  public ResponseEntity<Map<String, Object>> coolingOff(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("beneficiaryId") Long beneficiaryId,
      @RequestParam("owner") Long owningCustomerId
  ) {
    String cid = ensureCorrelationId(correlationId);
    var status = service.getCoolingOffStatus(owningCustomerId, beneficiaryId);
    Map<String, Object> body = new HashMap<>();
    body.put("withinCoolingOff", status.withinCoolingOff());
    body.put("availableAt", status.availableAt());
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(body);
  }

  @PostMapping("/internal/dev/beneficiaries/{beneficiaryId}/risk-score")
  public ResponseEntity<Void> setRiskScoreDev(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("beneficiaryId") Long beneficiaryId,
      @Valid @RequestBody RiskScoreRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.setRiskScoreDev(beneficiaryId, request.getScore(), cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .build();
  }

  private String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
