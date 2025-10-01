package com.omnibank.beneficiary.repository;

import com.omnibank.beneficiary.domain.OtpChallenge;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {
  Optional<OtpChallenge> findFirstByBeneficiaryIdOrderByIdDesc(Long beneficiaryId);

  default boolean isExpired(OtpChallenge c) {
    return c.getExpiresAt() != null && Instant.now().isAfter(c.getExpiresAt());
  }
}
