package com.omnibank.ledger.application;

import com.omnibank.ledger.domain.LedgerEntry;
import com.omnibank.ledger.domain.LedgerTransaction;
import com.omnibank.ledger.repository.LedgerTransactionRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.omnibank.ledger.events.EventPublisher;
import com.omnibank.ledger.events.EventTypes;
import com.omnibank.ledger.config.AppProperties;

/**
 * Ledger posting service enforcing double-entry constraints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

  private final LedgerTransactionRepository txRepo;
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  /**
   * Posts a financial transaction atomically. Enforces:
   * - At least 2 entries
   * - All amounts > 0
   * - Directions only 'D' or 'C'
   * - Sum(debits) == Sum(credits)
   */
  @Transactional
  public PostResult postTransaction(@NotBlank String transactionType,
                                    @NotNull List<Entry> entries,
                                    String correlationId) {
    validate(transactionType, entries);

    String txUuid = UUID.randomUUID().toString();
    LedgerTransaction tx = LedgerTransaction.builder()
        .transactionUuid(txUuid)
        .transactionType(transactionType.toUpperCase())
        .status("POSTED")
        .build();

    List<LedgerEntry> toPersist = new ArrayList<>();
    for (Entry e : entries) {
      LedgerEntry le = LedgerEntry.builder()
          .transaction(tx)
          .accountNumber(e.getAccountNumber())
          .amount(e.getAmount())
          .direction(String.valueOf(Character.toUpperCase(e.getDirection())))
          .build();
      toPersist.add(le);
    }
    tx.setEntries(toPersist);

    txRepo.save(tx);

    log.info("Ledger POSTED txUuid={} type={} entries={} correlationId={}",
        txUuid, transactionType, entries.size(), correlationId);

    // Publish TransactionPosted event
    eventPublisher.publish(
        props.getEvents().getTopic(),
        EventTypes.TRANSACTION_POSTED,
        new EventPayload(
            txUuid,
            entries.stream()
                .map(e -> new EventPayload.PostedEntry(
                    e.getAccountNumber(),
                    e.getAmount(),
                    Character.toUpperCase(e.getDirection())
                ))
                .toList()
        ),
        correlationId
    );

    return new PostResult(txUuid, "POSTED");
  }

  private static void validate(String transactionType, List<Entry> entries) {
    if (transactionType == null || transactionType.isBlank()) {
      throw new IllegalArgumentException("transactionType is required");
    }
    if (entries == null || entries.size() < 2) {
      throw new IllegalArgumentException("At least two entries are required");
    }
    BigDecimal debits = BigDecimal.ZERO;
    BigDecimal credits = BigDecimal.ZERO;

    for (Entry e : entries) {
      if (e == null) {
        throw new IllegalArgumentException("entry cannot be null");
      }
      if (e.getAccountNumber() == null || e.getAccountNumber().isBlank()) {
        throw new IllegalArgumentException("entry.accountNumber is required");
      }
      if (e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("entry.amount must be > 0");
      }
      char dir = Character.toUpperCase(e.getDirection());
      if (dir != 'D' && dir != 'C') {
        throw new IllegalArgumentException("entry.direction must be 'D' or 'C'");
      }
      if (dir == 'D') {
        debits = debits.add(e.getAmount());
      } else {
        credits = credits.add(e.getAmount());
      }
    }
    if (debits.compareTo(credits) != 0) {
      throw new IllegalArgumentException("Sum of debits must equal sum of credits");
    }
  }

  @Value
  public static class Entry {
    String accountNumber;
    BigDecimal amount;
    char direction; // 'D' or 'C'
  }

  @Value
  public static class PostResult {
    String transactionId;
    String status;
  }

  @Value
  public static class EventPayload {
    String transactionId;
    List<PostedEntry> entries;

    @Value
    public static class PostedEntry {
      String account;
      BigDecimal amount;
      char direction;
    }
  }
}
