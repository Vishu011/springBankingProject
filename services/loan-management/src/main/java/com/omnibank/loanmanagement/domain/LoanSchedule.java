package com.omnibank.loanmanagement.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "LOAN_SCHEDULE", indexes = {
    @Index(name = "IX_SCH_LOAN", columnList = "LOAN_ACCOUNT_ID"),
    @Index(name = "IX_SCH_STATUS", columnList = "PAYMENT_STATUS")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "LOAN_ACCOUNT_ID", nullable = false)
  private LoanAccount loanAccount;

  @Column(name = "SEQ_NO", nullable = false)
  private Integer seqNo; // 1..N

  @Column(name = "DUE_DATE", nullable = false)
  private Instant dueDate;

  @Column(name = "EMI_AMOUNT", precision = 19, scale = 4, nullable = false)
  private BigDecimal emiAmount;

  @Column(name = "PRINCIPAL_COMPONENT", precision = 19, scale = 4, nullable = false)
  private BigDecimal principalComponent;

  @Column(name = "INTEREST_COMPONENT", precision = 19, scale = 4, nullable = false)
  private BigDecimal interestComponent;

  // Amounts paid against this schedule (track partials)
  @Column(name = "INTEREST_PAID", precision = 19, scale = 4, nullable = false)
  private BigDecimal interestPaid;

  @Column(name = "PRINCIPAL_PAID", precision = 19, scale = 4, nullable = false)
  private BigDecimal principalPaid;

  @Column(name = "PAYMENT_STATUS", length = 20, nullable = false)
  private String paymentStatus; // PENDING | PAID

  @Column(name = "PAID_AT")
  private Instant paidAt;
}
