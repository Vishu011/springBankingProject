package com.omnibank.loanmanagement.application;

import com.omnibank.loanmanagement.LoanManagementApplication;
import com.omnibank.loanmanagement.domain.LoanAccount;
import com.omnibank.loanmanagement.domain.LoanSchedule;
import com.omnibank.loanmanagement.repository.LoanAccountRepository;
import com.omnibank.loanmanagement.repository.LoanScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LoanManagementApplication.class)
public class LoanManagementServiceTest {

  @Autowired
  private LoanManagementService service;

  @Autowired
  private LoanAccountRepository loanRepo;

  @Autowired
  private LoanScheduleRepository scheduleRepo;

  private static final BigDecimal PRINCIPAL = new BigDecimal("12000.00");
  private static final BigDecimal INTEREST_RATE = new BigDecimal("12.00"); // 12% annual

  private String appId;

  @BeforeEach
  void init() {
    appId = "APP-" + System.nanoTime();
    LoanManagementService.LoanApprovedPayload payload = new LoanManagementService.LoanApprovedPayload(
        appId, 1001L, "PERSONAL", PRINCIPAL, INTEREST_RATE, "TestAgent"
    );
    service.handleLoanApproved(payload, "cid-loan-1");
  }

  @Test
  void schedule_generation_sums_principal_and_has_expected_terms() {
    LoanAccount acc = loanRepo.findByApplicationId(appId).orElseThrow();
    List<LoanSchedule> sch = scheduleRepo.findByLoanAccountOrderBySeqNoAsc(acc);
    assertEquals(12, sch.size(), "Default tenure should be 12 months");

    BigDecimal sumPrincipal = sch.stream()
        .map(LoanSchedule::getPrincipalComponent)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, BigDecimal.ROUND_HALF_UP);

    // Sum of principal components should equal the approved principal
    assertEquals(0, sumPrincipal.compareTo(PRINCIPAL.setScale(2, BigDecimal.ROUND_HALF_UP)));
  }

  @Test
  void applyEmi_partial_interest_first_then_principal_and_idempotent() {
    LoanAccount acc = loanRepo.findByApplicationId(appId).orElseThrow();

    // Get initial next EMI and components for verification
    List<LoanManagementService.LoanScheduleView> before = service.getScheduleByLoanAccount(acc.getLoanAccountNumber());
    LoanManagementService.LoanScheduleView first = before.get(0);
    BigDecimal firstEmi = first.emiAmount();
    BigDecimal firstInterest = first.interestComponent();
    BigDecimal firstPrincipal = first.principalComponent();

    // Apply less than interest -> should only reduce interestPaid, no principalPaid, no balance reduction
    BigDecimal partialInterest = firstInterest.divide(new BigDecimal("2"), 2, BigDecimal.ROUND_HALF_UP);
    String tx1 = "TX-" + System.nanoTime();
    service.applyEmiPayment(tx1, acc.getLoanAccountNumber(), partialInterest);

    List<LoanManagementService.LoanScheduleView> after1 = service.getScheduleByLoanAccount(acc.getLoanAccountNumber());
    LoanManagementService.LoanScheduleView firstAfter1 = after1.get(0);

    // interest paid should increase, principal paid remains 0.00, balance unchanged
    assertTrue(firstAfter1.paymentStatus().equalsIgnoreCase("PENDING"));
    // We don't expose interestPaid/principalPaid on view, so validate through balance not changing
    LoanManagementService.LoanAccountView accAfter1 = service.getByLoanAccountNumber(acc.getLoanAccountNumber());
    assertEquals(0, acc.getPrincipalDisbursed().compareTo(accAfter1.currentBalance()));

    // Apply remaining to cover rest of interest + some principal
    BigDecimal remainingToCoverInterest = firstInterest.subtract(partialInterest);
    BigDecimal somePrincipal = new BigDecimal("10.00");
    String tx2 = "TX-" + System.nanoTime();
    service.applyEmiPayment(tx2, acc.getLoanAccountNumber(), remainingToCoverInterest.add(somePrincipal));

    LoanManagementService.LoanAccountView accAfter2 = service.getByLoanAccountNumber(acc.getLoanAccountNumber());
    // Balance should be reduced exactly by the principal portion paid (somePrincipal)
    assertEquals(0, acc.getPrincipalDisbursed().subtract(somePrincipal).compareTo(accAfter2.currentBalance()));

    // Idempotency: reapply tx2 should have no effect
    service.applyEmiPayment(tx2, acc.getLoanAccountNumber(), remainingToCoverInterest.add(somePrincipal));
    LoanManagementService.LoanAccountView accAfter2Dup = service.getByLoanAccountNumber(acc.getLoanAccountNumber());
    assertEquals(0, accAfter2.currentBalance().compareTo(accAfter2Dup.currentBalance()));

    // If we fully pay remaining principal component for first EMI, status should flip to PAID
    BigDecimal principalLeft = firstPrincipal.subtract(somePrincipal);
    String tx3 = "TX-" + System.nanoTime();
    service.applyEmiPayment(tx3, acc.getLoanAccountNumber(), principalLeft);

    List<LoanManagementService.LoanScheduleView> after3 = service.getScheduleByLoanAccount(acc.getLoanAccountNumber());
    assertEquals("PAID", after3.get(0).paymentStatus());
  }

  @Test
  void loan_summary_reports_outstanding_and_next_emi() {
    LoanAccount acc = loanRepo.findByApplicationId(appId).orElseThrow();
    LoanManagementService.LoanSummaryView summary = service.getLoanSummary(acc.getLoanAccountNumber());
    assertNotNull(summary);
    assertEquals(acc.getLoanAccountNumber(), summary.loanAccountNumber());
    assertTrue(summary.outstandingPrincipal().compareTo(BigDecimal.ZERO) > 0);
    assertNotNull(summary.nextSeqNo());
  }
}
