package com.omnibank.beneficiary.domain;

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
@Table(name = "BENEFICIARIES", indexes = {
    @Index(name = "IX_BEN_OWNER", columnList = "OWNING_CUSTOMER_ID")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "BENEFICIARY_ID")
  private Long id;

  @Column(name = "OWNING_CUSTOMER_ID", nullable = false)
  private Long owningCustomerId;

  @Column(name = "NICKNAME", length = 100, nullable = false)
  private String nickname;

  @Column(name = "ACCOUNT_NUMBER", length = 40, nullable = false)
  private String accountNumber;

  @Column(name = "BANK_CODE", length = 20, nullable = false)
  private String bankCode;

  // PENDING_OTP, ACTIVE, BLOCKED
  @Column(name = "STATUS", length = 20, nullable = false)
  private String status;

  @Column(name = "ADDED_TS")
  private Instant addedTs;

  @Column(name = "RISK_SCORE", precision = 5, scale = 4)
  private BigDecimal riskScore;

  @CreationTimestamp
  @Column(name = "CREATED_AT", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "UPDATED_AT")
  private Instant updatedAt;
}
