package com.omnibank.ledger.api;

import com.omnibank.ledger.domain.LedgerEntry;
import com.omnibank.ledger.repository.LedgerEntryRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Query API for ledger account transaction history.
 * GET /api/v1/accounts/{accountNumber}/history?size=50
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountHistoryController {

  public static final String HDR_CORRELATION_ID = "X-Correlation-Id";

  private final LedgerEntryRepository entryRepository;

  @GetMapping("/{accountNumber}/history")
  public ResponseEntity<List<HistoryItem>> getHistory(
      @RequestHeader(value = HDR_CORRELATION_ID, required = false) String correlationId,
      @PathVariable("accountNumber") @NotBlank String accountNumber,
      @RequestParam(name = "size", required = false, defaultValue = "50") @Min(1) @Max(500) int size
  ) {
    String cid = ensureCorrelationId(correlationId);
    List<LedgerEntry> entries = entryRepository.findHistoryByAccount(accountNumber);
    List<HistoryItem> items = entries.stream()
        .limit(size)
        .map(e -> new HistoryItem(
            e.getTransaction().getTransactionUuid(),
            e.getTransaction().getPostedTs(),
            e.getAmount(),
            firstChar(e.getDirection())
        ))
        .collect(Collectors.toList());

    return ResponseEntity.ok()
        .header(HDR_CORRELATION_ID, cid)
        .body(items);
  }

  private static String ensureCorrelationId(String v) {
    return StringUtils.hasText(v) ? v : java.util.UUID.randomUUID().toString();
  }

  private static char firstChar(String s) {
    return (s != null && !s.isBlank()) ? Character.toUpperCase(s.charAt(0)) : ' ';
  }

  @Value
  public static class HistoryItem {
    String transactionId;  // transaction UUID
    Instant postedAt;
    BigDecimal amount;
    char direction;        // 'D' | 'C'
  }
}
