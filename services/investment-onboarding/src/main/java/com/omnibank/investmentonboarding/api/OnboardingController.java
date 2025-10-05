package com.omnibank.investmentonboarding.api;

import com.omnibank.investmentonboarding.application.OnboardingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investments/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final OnboardingService service;

  @PostMapping("/start")
  public ResponseEntity<OnboardingService.View> start(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody StartRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    var view = service.start(request.getCustomerId(), request.getRiskLevel(), request.getAnswers(), cid);
    return ResponseEntity.accepted().header(HDR_CORRELATION_ID, cid).body(view);
  }

  @PostMapping("/{profileId}/activate")
  public ResponseEntity<OnboardingService.View> activate(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("profileId") @NotBlank String profileId
  ) {
    String cid = ensureCorrelationId(correlationId);
    var view = service.activate(profileId, cid);
    return ResponseEntity.ok().header(HDR_CORRELATION_ID, cid).body(view);
  }

  @GetMapping("/{profileId}")
  public ResponseEntity<OnboardingService.View> get(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("profileId") @NotBlank String profileId
  ) {
    String cid = ensureCorrelationId(correlationId);
    var view = service.get(profileId);
    return ResponseEntity.ok().header(HDR_CORRELATION_ID, cid).body(view);
  }

  private static String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }

  @Data
  public static class StartRequest {
    @NotNull
    private Long customerId;
    @NotBlank
    private String riskLevel; // e.g., "CONSERVATIVE" | "BALANCED" | "AGGRESSIVE"
    private Map<String, Object> answers; // questionnaire answers (dev-open)
  }
}
