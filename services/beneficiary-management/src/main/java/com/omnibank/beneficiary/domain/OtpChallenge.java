package com.omnibank.beneficiary.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OTP_CHALLENGES", indexes = {
    @Index(name = "IX_OTP_BENEFICIARY", columnList = "BENEFICIARY_ID")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpChallenge {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "CHALLENGE_ID")
  private Long id;

  @Column(name = "BENEFICIARY_ID", nullable = false)
  private Long beneficiaryId;

  // store a hash of the OTP code in real systems; for dev store as plain if needed
  @Column(name = "CODE_HASH", length = 128, nullable = false)
  private String codeHash;

  @Column(name = "EXPIRES_AT", nullable = false)
  private Instant expiresAt;

  @Column(name = "ATTEMPTS", nullable = false)
  private int attempts;

  @Column(name = "MAX_ATTEMPTS", nullable = false)
  private int maxAttempts;
}
