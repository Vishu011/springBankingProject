package com.omnibank.loanorigination.api;

import com.omnibank.loanorigination.application.LoanOriginationService;
import com.omnibank.loanorigination.application.LoanOriginationService.ApplicationView;
import com.omnibank.loanorigination.application.LoanOriginationService.DecisionParams;
import com.omnibank.loanorigination.application.LoanOriginationService.Created;
import com.omnibank.loanorigination.application.LoanOriginationService.History;
import com.omnibank.loanorigination.application.LoanOriginationService.StartParams;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Loan Origination REST API (dev-local in-memory MVP).
 * - POST   /api/v1/loans/applications
 * - GET    /api/v1/loans/applications/{appId}
 * - POST   /api/v1/loans/agent/record-decision
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanOriginationController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final LoanOriginationService service;

  @PostMapping("/applications")
  public ResponseEntity<Created> createApplication(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody StartRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    Created res = service.createApplication(
        new StartParams(request.getCustomerId(), request.getLoanType(), request.getAmount()),
        cid
    );
    return ResponseEntity.accepted()
        .header(HDR_CORRELATION_ID, cid)
        .body(res);
  }

  @GetMapping("/applications/{appId}")
  public ResponseEntity<ApplicationView> getApplication(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("appId") String appId
  ) {
    String cid = ensureCorrelationId(correlationId);
    ApplicationView view = service.getApplication(appId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @PostMapping("/agent/record-decision")
  public ResponseEntity<Void> recordDecision(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody DecisionRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.recordDecision(
        new DecisionParams(
            request.getApplicationId(),
            request.getDecision(),
            request.getApprovedAmount(),
            request.getInterestRate(),
            request.getDecisionBy()
        ),
        cid
    );
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .build();
  }

  private static String ensureCorrelationId(String v) {
    return StringUtils.hasText(v) ? v : java.util.UUID.randomUUID().toString();
  }

  // DTOs

  @Data
  public static class StartRequest {
    @NotNull
    private Long customerId;

    @NotBlank
    private String loanType; // PERSONAL, HOME, AUTO, etc.

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;
  }

  @Data
  public static class DecisionRequest {
    @NotBlank
    private String applicationId;

    @NotBlank
    private String decision; // APPROVED | REJECTED

    // Required if APPROVED
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;

    private String decisionBy;
  }

  // Re-expose view types here if needed by Swagger (compile-time reference)
  public record ApplicationViewDto(
      String applicationId,
      String status,
      String loanType,
      BigDecimal requestedAmount,
      BigDecimal approvedAmount,
      BigDecimal interestRate,
      String decisionBy,
      Instant createdAt,
      List<History> underwritingHistory
  ) {}
}
