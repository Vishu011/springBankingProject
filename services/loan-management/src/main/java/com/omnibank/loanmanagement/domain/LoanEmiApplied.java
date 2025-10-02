package com.omnibank.loanmanagement.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "LOAN_EMI_APPLIED", indexes = {
    @Index(name = "IX_TX_UNQ", columnList = "TRANSACTION_ID", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanEmiApplied {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "TRANSACTION_ID", length = 64, nullable = false, unique = true)
  private String transactionId;

  @Column(name = "LOAN_ACCOUNT_NUMBER", length = 32, nullable = false)
  private String loanAccountNumber;

  @Column(name = "APPLIED_AT", nullable = false, updatable = false)
  private Instant appliedAt;

  @PrePersist
  void prePersist() {
    if (appliedAt == null) appliedAt = Instant.now();
  }
}
