package com.omnibank.ledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Idempotency marker for processed payment approvals to avoid double-posting
 * the same PaymentApprovedForProcessing event to the ledger.
 */
@Entity
@Table(name = "PROCESSED_PAYMENTS", indexes = {
    @Index(name = "IX_PAYMENT_UUID_UNQ", columnList = "PAYMENT_UUID", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedPayment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "PAYMENT_UUID", length = 64, nullable = false, unique = true)
  private String paymentUuid;

  @Column(name = "PROCESSED_AT", nullable = false, updatable = false)
  private Instant processedAt;

  @PrePersist
  void prePersist() {
    if (processedAt == null) {
      processedAt = Instant.now();
    }
  }
}
