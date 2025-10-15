package com.bank.aiorchestrator.workflow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bank.aiorchestrator.integrations.account.AccountAdminClient;
import com.bank.aiorchestrator.integrations.account.dto.AccountResponse;
import com.bank.aiorchestrator.integrations.loan.LoanAdminClient;
import com.bank.aiorchestrator.integrations.loan.dto.LoanRejectionRequest;
import com.bank.aiorchestrator.integrations.loan.dto.LoanResponseDto;
import com.bank.aiorchestrator.model.AgentMode;
import com.bank.aiorchestrator.service.AgentStateService;
import com.bank.aiorchestrator.service.AuditService;
import com.bank.aiorchestrator.service.QueueMetricsService;

/**
 * Automates Loan application review per business policy.
 *
 * Policy (interpreted and normalized):
 * - If user has any SALARY_CORPORATE account AND joined balance across all accounts > 200,000
 *      => Approve up to 8,000,000; otherwise reject with reason.
 * - If user has NO SALARY_CORPORATE accounts:
 *      - If joined balance >= 50,000 => Approve up to 2,000,000
 *      - If joined balance < 50,000 => Approve up to 300,000
 *      Amounts above caps are rejected with reason.
 *
 * Notes:
 * - We rely on LoanService /loans returning all applications including PENDING.
 * - We filter by status == "PENDING".
 * - Account types come as strings; we match "SALARY_CORPORATE".
 */
@Service
public class LoanWorkflow {

    private static final Logger log = LoggerFactory.getLogger(LoanWorkflow.class);

    private static final String ACCOUNT_TYPE_SALARY = "SALARY_CORPORATE";

    private static final long BALANCE_200K = 200_000L;
    private static final long BALANCE_50K = 50_000L;

    private static final long CAP_CORPORATE = 8_000_000L; // 80,00,000
    private static final long CAP_NO_CORP_MID = 2_000_000L; // 20,00,000
    private static final long CAP_NO_CORP_LOW = 300_000L;   // 3,00,000

    private final LoanAdminClient loanClient;
    private final AccountAdminClient accountClient;
    private final AgentStateService agentStateService;
    private final QueueMetricsService queueMetricsService;
    private final AuditService auditService;

    public LoanWorkflow(LoanAdminClient loanClient,
                        AccountAdminClient accountClient,
                        AgentStateService agentStateService,
                        QueueMetricsService queueMetricsService,
                        AuditService auditService) {
        this.loanClient = loanClient;
        this.accountClient = accountClient;
        this.agentStateService = agentStateService;
        this.queueMetricsService = queueMetricsService;
        this.auditService = auditService;
    }

    public void process() {
        List<LoanResponseDto> all = null;
        try {
            all = loanClient.getAllLoans();
        } catch (Exception ex) {
            log.error("Loans: failed to fetch applications: {}", ex.getMessage());
            return;
        }
        if (all == null || all.isEmpty()) {
            log.debug("Loans: no applications found");
            return;
        }

        int pendingCount = 0;
        for (LoanResponseDto loan : all) {
            if (!"PENDING".equalsIgnoreCase(s(loan.getStatus()))) {
                continue;
            }
            pendingCount++;

            Decision decision = decide(loan);

            // Simple idempotency: skip if we already processed same evidence for this loan
            String evidenceHash = auditService.evidenceHash(Map.of(
                    "loanId", loan.getLoanId(),
                    "userId", loan.getUserId(),
                    "amount", toLong(loan.getAmount()),
                    "status", s(loan.getStatus())
            ));
            if (auditService.isDuplicate("loans", loan.getLoanId(), evidenceHash)) {
                log.debug("Loans: duplicate evidence for loan {}, skipping", loan.getLoanId());
                continue;
            }
            if (agentStateService.getMode() == AgentMode.DRY_RUN) {
                log.info("Loans[DRY_RUN]: loanId={} user={} decision={} comment='{}'",
                        loan.getLoanId(), loan.getUserId(), decision.action, decision.comment);
                continue;
            }
            if (agentStateService.getMode() == AgentMode.OFF) {
                log.debug("Loans: Agent mode OFF; skipping reviews");
                break;
            }

            try {
                if (decision.action == Action.APPROVE) {
                    var updated = loanClient.approve(loan.getLoanId());
                    log.info("Loans: approved loan {} for user {} -> status={}", loan.getLoanId(), loan.getUserId(), s(updated.getStatus()));
                    auditService.record("loans", loan.getLoanId(), loan.getUserId(), "APPROVED",
                            decision.comment, evidenceHash, String.valueOf(agentStateService.getMode()));
                } else {
                    var updated = loanClient.reject(loan.getLoanId(), new LoanRejectionRequest(decision.comment));
                    log.info("Loans: rejected loan {} for user {} -> status={} reason='{}'",
                            loan.getLoanId(), loan.getUserId(), s(updated.getStatus()), decision.comment);
                    auditService.record("loans", loan.getLoanId(), loan.getUserId(), "REJECTED",
                            decision.comment, evidenceHash, String.valueOf(agentStateService.getMode()));
                }
            } catch (Exception ex) {
                log.error("Loans: review failed for loan {}: {}", loan.getLoanId(), ex.getMessage());
            }
        }
        queueMetricsService.setQueueSize("loans", pendingCount);
    }

    private Decision decide(LoanResponseDto loan) {
        String userId = loan.getUserId();
        List<AccountResponse> accounts = null;
        try {
            accounts = accountClient.getAccountsByUser(userId);
        } catch (Exception ex) {
            String msg = "Could not retrieve accounts to evaluate loan. Rejecting conservatively.";
            log.warn("Loans: {} user={} error={}", msg, userId, ex.getMessage());
            return reject(msg);
        }
        long joinedBalance = sumBalances(accounts);
        boolean hasCorporate = hasCorporateAccount(accounts);
        long requested = toLong(loan.getAmount());

        if (hasCorporate && joinedBalance > BALANCE_200K) {
            if (requested <= CAP_CORPORATE) return approve(String.format(Locale.US,
                    "Auto-approved (corporate account present; joined balance %,d > 200,000; cap %,d).",
                    joinedBalance, CAP_CORPORATE));
            return reject(String.format(Locale.US,
                    "Requested amount %,d exceeds corporate cap %,d based on joined balance %,d.",
                    requested, CAP_CORPORATE, joinedBalance));
        }

        if (!hasCorporate) {
            if (joinedBalance >= BALANCE_50K) {
                if (requested <= CAP_NO_CORP_MID) return approve(String.format(Locale.US,
                        "Auto-approved (no corporate; joined balance %,d ≥ 50,000; cap %,d).",
                        joinedBalance, CAP_NO_CORP_MID));
                return reject(String.format(Locale.US,
                        "Requested amount %,d exceeds cap %,d for non-corporate profile with joined balance %,d.",
                        requested, CAP_NO_CORP_MID, joinedBalance));
            } else {
                if (requested <= CAP_NO_CORP_LOW) return approve(String.format(Locale.US,
                        "Auto-approved (no corporate; joined balance %,d < 50,000; cap %,d).",
                        joinedBalance, CAP_NO_CORP_LOW));
                return reject(String.format(Locale.US,
                        "Requested amount %,d exceeds cap %,d for non-corporate profile with joined balance %,d.",
                        requested, CAP_NO_CORP_LOW, joinedBalance));
            }
        }

        // Fallback conservative rule
        return reject("Profile does not meet approval criteria.");
    }

    private long sumBalances(List<AccountResponse> accounts) {
        if (accounts == null) return 0L;
        long total = 0L;
        for (AccountResponse acc : accounts) {
            Double bal = acc.getBalance();
            if (bal != null) total += Math.round(bal);
        }
        return total;
    }

    private boolean hasCorporateAccount(List<AccountResponse> accounts) {
        if (accounts == null) return false;
        for (AccountResponse acc : accounts) {
            String type = s(acc.getAccountType());
            if (ACCOUNT_TYPE_SALARY.equalsIgnoreCase(type)) return true;
        }
        return false;
    }

    private long toLong(BigDecimal v) {
        if (v == null) return 0L;
        try {
            return v.longValue();
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private String s(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Decision approve(String comment) { return new Decision(Action.APPROVE, comment); }
    private Decision reject(String comment)  { return new Decision(Action.REJECT, comment); }

    private enum Action { APPROVE, REJECT }
    private record Decision(Action action, String comment) {}
}
