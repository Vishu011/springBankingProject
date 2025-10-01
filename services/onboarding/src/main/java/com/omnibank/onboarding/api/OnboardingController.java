package com.omnibank.onboarding.api;

import com.omnibank.onboarding.api.dto.AgentUpdateStatusRequest;
import com.omnibank.onboarding.api.dto.DocumentUploadRequest;
import com.omnibank.onboarding.api.dto.StartOnboardingRequest;
import com.omnibank.onboarding.api.dto.StartOnboardingResponse;
import com.omnibank.onboarding.application.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for customer onboarding.
 * Endpoints (as per design doc):
 * - POST /api/v1/onboarding/start
 * - POST /api/v1/onboarding/{appId}/documents
 * - POST /api/v1/onboarding/agent/update-status
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final OnboardingService service;

  @PostMapping("/start")
  public ResponseEntity<StartOnboardingResponse> start(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody StartOnboardingRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    String applicationId = service.start(request, cid);
    return ResponseEntity.accepted().header(HDR_CORRELATION_ID, cid)
        .body(new StartOnboardingResponse(applicationId, "STARTED"));
  }

  @PostMapping("/{appId}/documents")
  public ResponseEntity<Void> uploadDocuments(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("appId") String appId,
      @Valid @RequestBody DocumentUploadRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.uploadDocuments(appId, request, cid);
    return ResponseEntity.accepted().header(HDR_CORRELATION_ID, cid).build();
  }

  // Secure/privileged (temporarily open in dev-local SecurityConfig)
  @PostMapping("/agent/update-status")
  public ResponseEntity<Void> agentUpdateStatus(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody AgentUpdateStatusRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.agentUpdate(request, cid);
    return ResponseEntity.ok().header(HDR_CORRELATION_ID, cid).build();
  }

  private String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
