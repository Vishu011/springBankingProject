package com.omnibank.loanmanagement.application;

import com.omnibank.loanmanagement.domain.LoanAccount;
import com.omnibank.loanmanagement.domain.LoanEmiApplied;
import com.omnibank.loanmanagement.domain.LoanSchedule;
import com.omnibank.loanmanagement.repository.LoanAccountRepository;
import com.omnibank.loanmanagement.repository.LoanEmiAppliedRepository;
import com.omnibank.loanmanagement.repository.LoanScheduleRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
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
  private final LoanScheduleRepository scheduleRepo;
  private final LoanEmiAppliedRepository emiAppliedRepo;

  private static final Random RAND = new SecureRandom();

  // Default tenure for MVP if not provided by origination (months)
  private static final int DEFAULT_TENURE_MONTHS = 12;

  @Transactional
  public void handleLoanApproved(LoanApprovedPayload payload, String correlationId) {
    if (payload == null) return;
    if (loanRepo.findByApplicationId(payload.applicationId()).isPresent()) {
      return; // idempotent
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
        .currentBalance(principal)
        .interestRate(rate)
        .status("ACTIVE")
        .build();

    acc = loanRepo.save(acc);

    // Generate a basic amortization schedule for MVP (equal EMI over DEFAULT_TENURE_MONTHS)
    generateSchedule(acc, principal, rate, DEFAULT_TENURE_MONTHS);
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

  @Transactional
  public List<LoanScheduleView> getScheduleByLoanAccount(String loanAccountNumber) {
    LoanAccount a = loanRepo.findByLoanAccountNumber(loanAccountNumber)
        .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountNumber));
    return scheduleRepo.findByLoanAccountOrderBySeqNoAsc(a).stream()
        .sorted(Comparator.comparing(LoanSchedule::getSeqNo))
        .map(this::toScheduleView)
        .collect(Collectors.toList());
  }

  // Apply EMI against a loan from a ledger TransactionPosted (idempotent by txId)
  @Transactional
  public void applyEmiPayment(String transactionId, String loanAccountNumber, BigDecimal amount) {
    if (transactionId == null || transactionId.isBlank() || loanAccountNumber == null || amount == null) {
      throw new IllegalArgumentException("Invalid EMI application input");
    }
    if (emiAppliedRepo.existsByTransactionId(transactionId)) {
      return; // idempotent
    }
    LoanAccount acc = loanRepo.findByLoanAccountNumber(loanAccountNumber)
        .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountNumber));

    // Find next pending schedule
    List<LoanSchedule> all = scheduleRepo.findByLoanAccountOrderBySeqNoAsc(acc);
    LoanSchedule pending = all.stream()
        .filter(s -> "PENDING".equalsIgnoreCase(s.getPaymentStatus()))
        .min(Comparator.comparing(LoanSchedule::getSeqNo))
        .orElse(null);

    if (pending == null) {
      // No pending schedule; treat entire amount as principal reduction
      acc.setCurrentBalance(acc.getCurrentBalance().subtract(amount).max(BigDecimal.ZERO));
      loanRepo.save(acc);
    } else {
      // Apply against interest first, then principal (track partials)
      BigDecimal remaining = amount;

      BigDecimal curInterestPaid = nvl(pending.getInterestPaid(), BigDecimal.ZERO);
      BigDecimal curPrincipalPaid = nvl(pending.getPrincipalPaid(), BigDecimal.ZERO);

      BigDecimal interestRemain = pending.getInterestComponent().subtract(curInterestPaid);
      if (interestRemain.compareTo(BigDecimal.ZERO) < 0) interestRemain = BigDecimal.ZERO;

      BigDecimal payInterest = remaining.min(interestRemain);
      if (payInterest.compareTo(BigDecimal.ZERO) > 0) {
        pending.setInterestPaid(curInterestPaid.add(payInterest));
        remaining = remaining.subtract(payInterest);
      }

      BigDecimal principalRemain = pending.getPrincipalComponent().subtract(curPrincipalPaid);
      if (principalRemain.compareTo(BigDecimal.ZERO) < 0) principalRemain = BigDecimal.ZERO;

      BigDecimal payPrincipal = remaining.min(principalRemain);
      if (payPrincipal.compareTo(BigDecimal.ZERO) > 0) {
        pending.setPrincipalPaid(curPrincipalPaid.add(payPrincipal));
        remaining = remaining.subtract(payPrincipal);
        // Only principal payments reduce outstanding balance
        acc.setCurrentBalance(acc.getCurrentBalance().subtract(payPrincipal).max(BigDecimal.ZERO));
      }

      // Mark schedule completed if both components fully paid
      if (pending.getInterestComponent().compareTo(nvl(pending.getInterestPaid(), BigDecimal.ZERO)) <= 0
          && pending.getPrincipalComponent().compareTo(nvl(pending.getPrincipalPaid(), BigDecimal.ZERO)) <= 0) {
        pending.setPaymentStatus("PAID");
        pending.setPaidAt(Instant.now());
      }

      scheduleRepo.save(pending);
      loanRepo.save(acc);

      // If any remainder exists (e.g., overpayment), reduce balance further
      if (remaining.compareTo(BigDecimal.ZERO) > 0) {
        acc.setCurrentBalance(acc.getCurrentBalance().subtract(remaining).max(BigDecimal.ZERO));
        loanRepo.save(acc);
      }
    }

    emiAppliedRepo.save(LoanEmiApplied.builder()
        .transactionId(transactionId)
        .loanAccountNumber(loanAccountNumber)
        .build());
  }

  private void generateSchedule(LoanAccount acc, BigDecimal principal, BigDecimal annualRatePct, int months) {
    // EMI formula with monthly rate r: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
    BigDecimal r = annualRatePct
        .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP); // monthly rate in decimal
    BigDecimal onePlusRPowerN = (BigDecimal.ONE.add(r)).pow(months);
    BigDecimal numerator = principal.multiply(r).multiply(onePlusRPowerN);
    BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
    BigDecimal emi = denominator.compareTo(BigDecimal.ZERO) == 0
        ? principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP)
        : numerator.divide(denominator, 2, RoundingMode.HALF_UP);

    BigDecimal balance = principal;
    LocalDate start = LocalDate.now(ZoneOffset.UTC).plusMonths(1);

    for (int i = 1; i <= months; i++) {
      BigDecimal interestComponent = balance.multiply(r).setScale(2, RoundingMode.HALF_UP);
      BigDecimal principalComponent = emi.subtract(interestComponent).setScale(2, RoundingMode.HALF_UP);
      if (i == months) {
        // adjust final principal to clear rounding
        principalComponent = balance;
        emi = principalComponent.add(interestComponent);
      }

      LoanSchedule sch = LoanSchedule.builder()
          .loanAccount(acc)
          .seqNo(i)
          .dueDate(start.plusMonths(i - 1).atStartOfDay().toInstant(ZoneOffset.UTC))
          .emiAmount(emi)
          .principalComponent(principalComponent)
          .interestComponent(interestComponent)
          .interestPaid(BigDecimal.ZERO)
          .principalPaid(BigDecimal.ZERO)
          .paymentStatus("PENDING")
          .build();
      scheduleRepo.save(sch);

      balance = balance.subtract(principalComponent);
      if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
    }
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

  private LoanScheduleView toScheduleView(LoanSchedule s) {
    return new LoanScheduleView(
        s.getSeqNo(),
        s.getDueDate(),
        s.getEmiAmount(),
        s.getPrincipalComponent(),
        s.getInterestComponent(),
        s.getPaymentStatus(),
        s.getPaidAt()
    );
  }

  private static String generateLoanAccountNumber() {
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

  // Query views
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

  public record LoanScheduleView(
      Integer seqNo,
      java.time.Instant dueDate,
      BigDecimal emiAmount,
      BigDecimal principalComponent,
      BigDecimal interestComponent,
      String paymentStatus,
      java.time.Instant paidAt
  ) {}
}
