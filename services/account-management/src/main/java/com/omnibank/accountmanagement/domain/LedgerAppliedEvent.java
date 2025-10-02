package com.omnibank.accountmanagement.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Idempotency marker for applied ledger TransactionPosted events.
 * Prevents double-applying the same transactionId to balances.
 */
@Entity
@Table(name = "LEDGER_APPLIED_EVENTS", indexes = {
    @Index(name = "IX_LEDGER_TX_UUID", columnList = "TRANSACTION_ID", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerAppliedEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "TRANSACTION_ID", length = 64, nullable = false, unique = true)
  private String transactionId;

  @Column(name = "APPLIED_AT", nullable = false, updatable = false)
  private Instant appliedAt;

  @PrePersist
  void prePersist() {
    if (appliedAt == null) {
      appliedAt = Instant.now();
    }
  }
}
