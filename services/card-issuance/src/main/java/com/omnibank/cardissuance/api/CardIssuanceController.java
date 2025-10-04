package com.omnibank.cardissuance.api;

import com.omnibank.cardissuance.application.CardIssuanceService;
import com.omnibank.cardissuance.application.CardIssuanceService.ApplicationView;
import com.omnibank.cardissuance.application.CardIssuanceService.Created;
import com.omnibank.cardissuance.application.CardIssuanceService.SubmitRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Card Issuance APIs (dev-open)
 * - POST /api/v1/cards/issuance/applications
 * - GET  /api/v1/cards/issuance/applications/{id}
 * - POST /api/v1/cards/issuance/applications/{id}/eligibility-check
 * - POST /api/v1/cards/issuance/applications/{id}/approve
 */
@RestController
@RequestMapping("/api/v1/cards/issuance")
@RequiredArgsConstructor
@Tag(name = "Card Issuance")
public class CardIssuanceController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final CardIssuanceService service;

  @PostMapping("/applications")
  @Operation(summary = "Submit card issuance application", responses = {
      @ApiResponse(responseCode = "202", description = "Application accepted"),
      @ApiResponse(responseCode = "400", description = "Validation error")
  })
  public ResponseEntity<Created> submit(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody SubmitRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    Created res = service.submit(request, cid);
    return ResponseEntity.accepted()
        .header(HDR_CORRELATION_ID, cid)
        .body(res);
  }

  @GetMapping("/applications/{id}")
  @Operation(summary = "Get issuance application by id")
  public ResponseEntity<ApplicationView> get(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("id") @NotBlank String applicationId
  ) {
    String cid = ensureCorrelationId(correlationId);
    ApplicationView view = service.get(applicationId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @PostMapping("/applications/{id}/eligibility-check")
  @Operation(summary = "Run eligibility check on application")
  public ResponseEntity<ApplicationView> eligibilityCheck(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("id") @NotBlank String applicationId
  ) {
    String cid = ensureCorrelationId(correlationId);
    ApplicationView view = service.eligibilityCheck(applicationId, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @PostMapping("/applications/{id}/approve")
  @Operation(summary = "Approve application (publishes CardApplicationApproved)")
  public ResponseEntity<ApplicationView> approve(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("id") @NotBlank String applicationId
  ) {
    String cid = ensureCorrelationId(correlationId);
    ApplicationView view = service.approve(applicationId, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  private static String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
