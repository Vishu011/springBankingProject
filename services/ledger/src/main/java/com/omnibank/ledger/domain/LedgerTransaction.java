package com.omnibank.ledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "TRANSACTIONS", indexes = {
    @Index(name = "IX_TX_UUID", columnList = "TRANSACTION_UUID", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "TRANSACTION_ID")
  private Long id;

  @Column(name = "TRANSACTION_UUID", length = 36, unique = true, nullable = false)
  private String transactionUuid;

  @Column(name = "TRANSACTION_TYPE", length = 20, nullable = false)
  private String transactionType; // e.g., "TRANSFER"

  @Column(name = "STATUS", length = 15, nullable = false)
  private String status; // e.g., "POSTED", "FAILED"

  @CreationTimestamp
  @Column(name = "POSTED_TS", updatable = false)
  private Instant postedTs;

  @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @Builder.Default
  private List<LedgerEntry> entries = new ArrayList<>();
}
