package com.omnibank.accountmanagement.api;

import com.omnibank.accountmanagement.api.dto.AdjustBalanceRequest;
import com.omnibank.accountmanagement.api.dto.CreateAccountRequest;
import com.omnibank.accountmanagement.api.dto.LedgerTransactionPostedDto;
import com.omnibank.accountmanagement.application.AccountManagementService;
import com.omnibank.accountmanagement.domain.Account;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final AccountManagementService service;

  @PostMapping("/accounts")
  public ResponseEntity<String> createAccount(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody CreateAccountRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    String accountNumber = service.createAccount(request.getCustomerId(), request.getAccountType(), cid);
    return ResponseEntity.accepted()
        .header(HDR_CORRELATION_ID, cid)
        .body(accountNumber);
  }

  @GetMapping("/customers/{customerId}/accounts")
  public ResponseEntity<List<Account>> listAccounts(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("customerId") Long customerId
  ) {
    String cid = ensureCorrelationId(correlationId);
    List<Account> list = service.listCustomerAccounts(customerId);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(list);
  }

  @GetMapping("/accounts/{accountNumber}/balance")
  public ResponseEntity<BigDecimal> getBalance(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("accountNumber") String accountNumber
  ) {
    String cid = ensureCorrelationId(correlationId);
    BigDecimal balance = service.getBalance(accountNumber);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(balance);
  }

  // Dev-only endpoint to adjust balance (simulate credits/debits)
  @PostMapping("/internal/dev/accounts/{accountNumber}/adjust-balance")
  public ResponseEntity<BigDecimal> adjustBalance(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("accountNumber") String accountNumber,
      @Valid @RequestBody AdjustBalanceRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    BigDecimal newBalance = service.adjustBalanceDev(accountNumber, request.getAmount(), cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(newBalance);
  }

  @PostMapping("/internal/dev/ledger-events/transaction-posted")
  public ResponseEntity<Void> applyLedgerTransactionPosted(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody LedgerTransactionPostedDto request
  ) {
    String cid = ensureCorrelationId(correlationId);
    service.applyLedgerTransactionPosted(request, cid);
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .build();
  }

  private String ensureCorrelationId(String headerValue) {
    return StringUtils.hasText(headerValue) ? headerValue : java.util.UUID.randomUUID().toString();
  }
}
