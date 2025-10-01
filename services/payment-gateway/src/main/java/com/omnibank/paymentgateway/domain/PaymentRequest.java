package com.omnibank.paymentgateway.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Persistence record of payment initiation requests and their orchestration status.
 */
@Entity
@Table(name = "PAYMENT_REQUESTS", indexes = {
    @Index(name = "IX_PAY_UUID", columnList = "PAYMENT_UUID", unique = true),
    @Index(name = "IX_PAY_CUSTOMER", columnList = "CUSTOMER_ID")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "PAYMENT_ID")
  private Long id;

  @Column(name = "PAYMENT_UUID", nullable = false, length = 36, unique = true)
  private String paymentUuid;

  @Column(name = "CUSTOMER_ID", nullable = false)
  private Long customerId;

  @Column(name = "FROM_ACCOUNT", nullable = false, length = 40)
  private String fromAccount;

  @Column(name = "TO_ACCOUNT", nullable = false, length = 40)
  private String toAccount;

  @Column(name = "AMOUNT", precision = 19, scale = 4, nullable = false)
  private BigDecimal amount;

  @Column(name = "CURRENCY", length = 3, nullable = false)
  private String currency;

  // RECEIVED | PROCESSING | MFA_CHALLENGE_REQUIRED | BLOCKED
  @Column(name = "STATUS", length = 30, nullable = false)
  private String status;

  @Column(name = "FRAUD_SCORE", precision = 5, scale = 4)
  private BigDecimal fraudScore;

  @Column(name = "FRAUD_ACTION_TAKEN", length = 20)
  private String fraudActionTaken;

  @CreationTimestamp
  @Column(name = "REQUEST_TS", updatable = false)
  private Instant requestTs;

  @UpdateTimestamp
  @Column(name = "UPDATED_AT")
  private Instant updatedAt;
}
