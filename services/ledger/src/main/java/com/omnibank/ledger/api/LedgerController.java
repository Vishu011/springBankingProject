package com.omnibank.ledger.api;

import com.omnibank.ledger.application.LedgerService;
import com.omnibank.ledger.application.LedgerService.Entry;
import com.omnibank.ledger.application.LedgerService.PostResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API for atomic ledger postings (dev-first).
 * - POST /api/v1/internal/ledger/transactions
 */
@RestController
@RequestMapping("/api/v1/internal/ledger")
@RequiredArgsConstructor
public class LedgerController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final LedgerService ledgerService;

  @PostMapping("/transactions")
  public ResponseEntity<PostResult> postTransaction(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @Valid @RequestBody PostTransactionRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);
    PostResult result = ledgerService.postTransaction(
        request.getTransactionType(),
        request.toEntries(),
        cid,
        request.getMetadata()
    );
    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(result);
  }

  @PostMapping("/loans/{loanAccount}/apply-emi")
  public ResponseEntity<PostResult> applyEmiViaLedger(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("loanAccount") @NotBlank String loanAccount,
      @Valid @RequestBody ApplyEmiRequest request
  ) {
    String cid = ensureCorrelationId(correlationId);

    // Build double-entry: debit customer's deposit account, credit loan account
    List<Entry> entries = List.of(
        new Entry(request.getFromAccount(), request.getAmount(), 'D'),
        new Entry(loanAccount, request.getAmount(), 'C')
    );

    Map<String, String> metadata = new HashMap<>();
    metadata.put("loanAccountNumber", loanAccount);

    PostResult result = ledgerService.postTransaction(
        "LOAN_EMI",
        entries,
        cid,
        metadata
    );

    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(result);
  }

  private static String ensureCorrelationId(String v) {
    return StringUtils.hasText(v) ? v : java.util.UUID.randomUUID().toString();
  }

  @Data
  public static class PostTransactionRequest {
    @NotBlank
    private String transactionType; // e.g., "TRANSFER"

    @NotEmpty
    private List<PostEntry> entries;

    // Optional metadata to flow into TransactionPosted (e.g., {"loanAccountNumber":"LN123..."})
    private Map<String, String> metadata;

    public List<Entry> toEntries() {
      return entries.stream()
          .map(e -> new Entry(e.getAccountNumber(), e.getAmount(), e.getDirection()))
          .toList();
    }
  }

  @Data
  public static class PostEntry {
    @NotBlank
    private String accountNumber;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    private BigDecimal amount;

    // Must be 'D' or 'C'
    @NotNull
    private Character direction;
  }

  @Data
  public static class ApplyEmiRequest {
    @NotBlank
    private String fromAccount;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    private BigDecimal amount;
  }
}
