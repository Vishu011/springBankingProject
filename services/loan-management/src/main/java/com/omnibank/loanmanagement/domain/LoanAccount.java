package com.omnibank.loanmanagement.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "LOAN_ACCOUNTS", indexes = {
    @Index(name = "IX_LN_APP_UNQ", columnList = "APPLICATION_ID", unique = true),
    @Index(name = "IX_LN_CUST", columnList = "CUSTOMER_ID")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "LOAN_ACCOUNT_NUMBER", length = 32, nullable = false, unique = true)
  private String loanAccountNumber;

  @Column(name = "APPLICATION_ID", length = 64, nullable = false, unique = true)
  private String applicationId;

  @Column(name = "CUSTOMER_ID", nullable = false)
  private Long customerId;

  @Column(name = "LOAN_TYPE", length = 32, nullable = false)
  private String loanType;

  @Column(name = "PRINCIPAL_DISBURSED", precision = 19, scale = 4, nullable = false)
  private BigDecimal principalDisbursed;

  @Column(name = "CURRENT_BALANCE", precision = 19, scale = 4, nullable = false)
  private BigDecimal currentBalance;

  @Column(name = "INTEREST_RATE", precision = 9, scale = 4, nullable = false)
  private BigDecimal interestRate; // annual percent e.g. 8.50

  @Column(name = "STATUS", length = 20, nullable = false)
  private String status; // ACTIVE | PAID_OFF | IN_ARREARS

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
    if (status == null) status = "ACTIVE";
  }
}
