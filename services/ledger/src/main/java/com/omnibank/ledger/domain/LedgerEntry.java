package com.omnibank.ledger.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TRANSACTION_ENTRIES", indexes = {
    @Index(name = "IX_ENTRY_ACCOUNT", columnList = "ACCOUNT_NUMBER")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ENTRY_ID")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "TRANSACTION_ID", nullable = false)
  private LedgerTransaction transaction;

  @Column(name = "ACCOUNT_NUMBER", length = 30, nullable = false)
  private String accountNumber;

  @Column(name = "AMOUNT", precision = 19, scale = 4, nullable = false)
  private BigDecimal amount;

  // 'D' for Debit, 'C' for Credit
  @Column(name = "DIRECTION", length = 1, nullable = false)
  private String direction; // single-char string to keep simple in JPA mapping
}
