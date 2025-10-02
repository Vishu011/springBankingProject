package com.omnibank.loanmanagement.application;

import com.omnibank.loanmanagement.domain.LoanAccount;
import com.omnibank.loanmanagement.repository.LoanAccountRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanManagementService {

  private final LoanAccountRepository loanRepo;
  private static final Random RAND = new SecureRandom();

  @Transactional
  public void handleLoanApproved(LoanApprovedPayload payload, String correlationId) {
    if (payload == null) return;
    // Idempotency: one loan account per origination applicationId
    if (loanRepo.findByApplicationId(payload.applicationId()).isPresent()) {
      return;
    }
    String loanAccNo = generateLoanAccountNumber();
    BigDecimal principal = nvl(payload.approvedAmount(), BigDecimal.ZERO);
    BigDecimal rate = nvl(payload.interestRate(), BigDecimal.ZERO);

    LoanAccount acc = LoanAccount.builder()
        .loanAccountNumber(loanAccNo)
        .applicationId(payload.applicationId())
        .customerId(payload.customerId())
        .loanType(upper(payload.loanType()))
        .principalDisbursed(principal)
        .currentBalance(principal) // initial balance equals principal at disbursal
        .interestRate(rate)
        .status("ACTIVE")
        .build();

    loanRepo.save(acc);
    // FUTURE: generate amortization schedule rows and persist
  }

  @Transactional
  public LoanAccountView getByLoanAccountNumber(String loanAccountNumber) {
    LoanAccount a = loanRepo.findByLoanAccountNumber(loanAccountNumber)
        .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountNumber));
    return toView(a);
  }

  @Transactional
  public List<LoanAccountView> getByCustomerId(Long customerId) {
    return loanRepo.findByCustomerId(customerId).stream()
        .map(this::toView)
        .collect(Collectors.toList());
  }

  private LoanAccountView toView(LoanAccount a) {
    return new LoanAccountView(
        a.getLoanAccountNumber(),
        a.getApplicationId(),
        a.getCustomerId(),
        a.getLoanType(),
        a.getPrincipalDisbursed(),
        a.getCurrentBalance(),
        a.getInterestRate(),
        a.getStatus(),
        a.getCreatedAt()
    );
  }

  private static String generateLoanAccountNumber() {
    // Simple dev-local generator: LN + 12 digits
    StringBuilder sb = new StringBuilder("LN");
    for (int i = 0; i < 12; i++) {
      sb.append(RAND.nextInt(10));
    }
    return sb.toString();
  }

  private static String upper(String s) {
    return s == null ? null : s.toUpperCase(Locale.ROOT);
  }

  private static BigDecimal nvl(BigDecimal v, BigDecimal d) {
    return v == null ? d : v;
  }

  // Payload matching loan-origination LoanApproved (extended to include customerId)
  public record LoanApprovedPayload(
      String applicationId,
      Long customerId,
      String loanType,
      BigDecimal approvedAmount,
      BigDecimal interestRate,
      String decisionBy
  ) {}

  // Query view DTO
  public record LoanAccountView(
      String loanAccountNumber,
      String applicationId,
      Long customerId,
      String loanType,
      BigDecimal principalDisbursed,
      BigDecimal currentBalance,
      BigDecimal interestRate,
      String status,
      java.time.Instant createdAt
  ) {}
}
