package com.otp.repository;

import com.otp.domain.OtpCode;
import com.otp.domain.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    Optional<OtpCode> findTopByUserIdAndPurposeAndContextIdAndConsumedAtIsNullOrderByCreatedAtDesc(
            String userId, OtpPurpose purpose, String contextId
    );

    Optional<OtpCode> findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String userId, OtpPurpose purpose
    );

    long countByUserIdAndPurposeAndCreatedAtAfter(String userId, OtpPurpose purpose, LocalDateTime createdAfter);

    @Query("select count(o) from OtpCode o where o.userId = ?1 and o.purpose = ?2 and o.createdAt > ?3 and (o.contextId = ?4 or (?4 is null and o.contextId is null))")
    long countRecentForContext(String userId, OtpPurpose purpose, LocalDateTime createdAfter, String contextId);
}
