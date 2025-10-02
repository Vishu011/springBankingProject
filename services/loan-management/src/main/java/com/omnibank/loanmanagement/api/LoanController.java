package com.omnibank.loanmanagement.api;

import com.omnibank.loanmanagement.application.LoanManagementService;
import com.omnibank.loanmanagement.application.LoanManagementService.LoanAccountView;
import com.omnibank.loanmanagement.application.LoanManagementService.LoanScheduleView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Loan Management APIs (MVP)
 * - GET  /api/v1/loans/{loanAccountNumber}
 * - GET  /api/v1/loans/customers/{customerId}
 * - GET  /api/v1/loans/{loanAccountNumber}/schedule
 * - POST /api/v1/internal/dev/loans/{loanAccountNumber}/apply-emi (dev)
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final LoanManagementService service;

  @GetMapping("/{loanAccountNumber}")
  public ResponseEntity<LoanAccountView> getByLoanAccountNumber(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("loanAccountNumber") @NotBlank String loanAccountNumber
  ) {
    String cid = ensureCorrelationId(correlationId);
    LoanAccountView view = service.getByLoanAccountNumber(loanAccountNumber);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(view);
  }

  @GetMapping("/customers/{customerId}")
  public ResponseEntity<List<LoanAccountView>> getByCustomerId(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("customerId") @NotNull Long customerId
  ) {
    String cid = ensureCorrelationId(correlationId);
    List<LoanAccountView> list = service.getByCustomerId(customerId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(list);
  }

  @GetMapping("/{loanAccountNumber}/schedule")
  public ResponseEntity<List<LoanScheduleView>> getSchedule(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("loanAccountNumber") @NotBlank String loanAccountNumber
  ) {
    String cid = ensureCorrelationId(correlationId);
    List<LoanScheduleView> list = service.getScheduleByLoanAccount(loanAccountNumber);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(list);
  }

  // Dev-only: simulate EMI application tied to a ledger transaction
  @PostMapping("/internal/dev/loans/{loanAccountNumber}/apply-emi")
  public ResponseEntity<Void> applyEmiDev(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("loanAccountNumber") @NotBlank String loanAccountNumber,
      @Valid @RequestBody ApplyEmiRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.applyEmiPayment(request.getTransactionId(), loanAccountNumber, request.getAmount());
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .build();
  }

  private static String ensureCorrelationId(String v) {
    return StringUtils.hasText(v) ? v : java.util.UUID.randomUUID().toString();
  }

  @Data
  public static class ApplyEmiRequest {
    @NotBlank
    private String transactionId;
    @NotNull
    private BigDecimal amount;
  }
}
