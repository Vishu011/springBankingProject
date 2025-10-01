package com.omnibank.beneficiary.application;

import com.omnibank.beneficiary.api.dto.CreateBeneficiaryRequest;
import com.omnibank.beneficiary.config.AppProperties;
import com.omnibank.beneficiary.domain.Beneficiary;
import com.omnibank.beneficiary.domain.OtpChallenge;
import com.omnibank.beneficiary.events.EventPublisher;
import com.omnibank.beneficiary.events.EventTypes;
import com.omnibank.beneficiary.repository.BeneficiaryRepository;
import com.omnibank.beneficiary.repository.OtpChallengeRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BeneficiaryManagementService {

  private final BeneficiaryRepository beneficiaryRepository;
  private final OtpChallengeRepository otpChallengeRepository;
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  public record CreateResult(Long beneficiaryId, Long challengeId, String otpDevEcho) {}

  @Transactional
  public CreateResult createBeneficiary(Long owningCustomerId, CreateBeneficiaryRequest req, String correlationId) {
    if (owningCustomerId == null || owningCustomerId <= 0) {
      throw new IllegalArgumentException("owningCustomerId must be provided");
    }
    Beneficiary b = Beneficiary.builder()
        .owningCustomerId(owningCustomerId)
        .nickname(req.getNickname())
        .accountNumber(req.getAccountNumber())
        .bankCode(req.getBankCode())
        .status("PENDING_OTP")
        .riskScore(null)
        .build();
    beneficiaryRepository.save(b);

    // generate OTP (6 digits), create challenge
    String otp = generateOtp();
    OtpChallenge c = OtpChallenge.builder()
        .beneficiaryId(b.getId())
        .codeHash(hash(otp)) // dev: simple hash
        .expiresAt(Instant.now().plusSeconds(props.getOtp().getTtlSeconds()))
        .attempts(0)
        .maxAttempts(5)
        .build();
    otpChallengeRepository.save(c);

    // publish BeneficiaryAdded (added into system, not yet active)
    publish(EventTypes.BENEFICIARY_ADDED,
        new EventPayloads.BeneficiaryAdded(b.getId(), b.getOwningCustomerId(), b.getAccountNumber(), b.getBankCode(), b.getNickname()),
        correlationId);

    // In real system, send OTP via SMS/email. For dev, echo back OTP to aid testing.
    return new CreateResult(b.getId(), c.getId(), otp);
  }

  @Transactional
  public void verifyOtp(Long owningCustomerId, Long beneficiaryId, Long challengeId, String code, String correlationId) {
    Beneficiary b = beneficiaryRepository.findByIdAndOwningCustomerId(beneficiaryId, owningCustomerId)
        .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found for owner " + owningCustomerId));
    OtpChallenge c = otpChallengeRepository.findById(challengeId)
        .orElseThrow(() -> new ResourceNotFoundException("OTP challenge not found: " + challengeId));

    if (!Objects.equals(c.getBeneficiaryId(), b.getId())) {
      throw new IllegalArgumentException("Challenge does not belong to the beneficiary");
    }
    if (otpChallengeRepository.isExpired(c)) {
      throw new IllegalArgumentException("OTP expired");
    }
    if (c.getAttempts() >= c.getMaxAttempts()) {
      throw new IllegalArgumentException("Max attempts exceeded");
    }

    c.setAttempts(c.getAttempts() + 1);
    otpChallengeRepository.save(c);

    if (!hash(code).equals(c.getCodeHash())) {
      throw new IllegalArgumentException("Invalid OTP");
    }

    // Success
    b.setStatus("ACTIVE");
    b.setAddedTs(Instant.now());
    beneficiaryRepository.save(b);

    publish(EventTypes.BENEFICIARY_ACTIVATED,
        new EventPayloads.BeneficiaryActivated(b.getId(), b.getOwningCustomerId(), b.getAddedTs()),
        correlationId);
  }

  @Transactional(readOnly = true)
  public List<Beneficiary> list(Long owningCustomerId) {
    return beneficiaryRepository.findByOwningCustomerIdOrderByCreatedAtDesc(owningCustomerId);
  }

  public record CoolingOffStatus(boolean withinCoolingOff, Instant availableAt) {}

  @Transactional(readOnly = true)
  public CoolingOffStatus getCoolingOffStatus(Long owningCustomerId, Long beneficiaryId) {
    Beneficiary b = beneficiaryRepository.findByIdAndOwningCustomerId(beneficiaryId, owningCustomerId)
        .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));
    int hours = props.getCoolingOff().getHours();
    if (b.getAddedTs() == null) {
      return new CoolingOffStatus(true, null);
    }
    Instant availableAt = b.getAddedTs().plusSeconds(hours * 3600L);
    boolean within = Instant.now().isBefore(availableAt);
    return new CoolingOffStatus(within, availableAt);
  }

  @Transactional
  public void setRiskScoreDev(Long beneficiaryId, BigDecimal score, String correlationId) {
    Beneficiary b = beneficiaryRepository.findById(beneficiaryId)
        .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + beneficiaryId));
    b.setRiskScore(score);
    beneficiaryRepository.save(b);

    publish(EventTypes.BENEFICIARY_RISK_SCORE_UPDATED,
        new EventPayloads.BeneficiaryRiskScoreUpdated(b.getId(), b.getOwningCustomerId(), score),
        correlationId);
  }

  private void publish(String type, Object payload, String correlationId) {
    eventPublisher.publish(props.getEvents().getTopic(), type, payload, correlationId);
  }

  private static String generateOtp() {
    SecureRandom r = new SecureRandom();
    return String.format("%06d", r.nextInt(1_000_000));
  }

  private static String hash(String s) {
    // DEV-ONLY simple hash; DO NOT USE in production.
    return Integer.toHexString(Objects.hash(s));
  }

  public static class EventPayloads {
    public record BeneficiaryAdded(Long beneficiaryId, Long owningCustomerId, String accountNumber, String bankCode, String nickname) {}
    public record BeneficiaryActivated(Long beneficiaryId, Long owningCustomerId, Instant activatedAt) {}
    public record BeneficiaryRiskScoreUpdated(Long beneficiaryId, Long owningCustomerId, BigDecimal riskScore) {}
  }

  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
  }
}
