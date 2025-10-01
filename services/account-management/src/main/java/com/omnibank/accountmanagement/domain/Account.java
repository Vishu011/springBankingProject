package com.omnibank.accountmanagement.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "ACCOUNTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

  @Id
  @Column(name = "ACCOUNT_NUMBER", length = 30)
  private String accountNumber;

  @Column(name = "CUSTOMER_ID", nullable = false)
  private Long customerId;

  @Column(name = "ACCOUNT_TYPE", length = 30, nullable = false)
  private String accountType; // SAVINGS, CURRENT, FD, RD

  @Column(name = "STATUS", length = 20, nullable = false)
  private String status; // ACTIVE, CLOSED, DORMANT

  @Column(name = "BALANCE", precision = 19, scale = 4, nullable = false)
  private BigDecimal balance;

  @Column(name = "OPENING_DATE", nullable = false)
  private Instant openingDate;

  @CreationTimestamp
  @Column(name = "CREATED_AT", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "UPDATED_AT")
  private Instant updatedAt;
}
