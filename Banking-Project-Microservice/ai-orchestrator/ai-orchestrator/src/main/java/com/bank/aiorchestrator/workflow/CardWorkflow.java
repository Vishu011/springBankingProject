package com.bank.aiorchestrator.workflow;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bank.aiorchestrator.ai.AiReasoner;
import com.bank.aiorchestrator.integrations.account.AccountAdminClient;
import com.bank.aiorchestrator.integrations.account.dto.AccountResponse;
import com.bank.aiorchestrator.integrations.card.CardAdminClient;
import com.bank.aiorchestrator.integrations.card.dto.CardApplicationResponse;
import com.bank.aiorchestrator.integrations.card.dto.ReviewCardApplicationRequest;
import com.bank.aiorchestrator.model.AgentMode;
import com.bank.aiorchestrator.service.AgentStateService;
import com.bank.aiorchestrator.service.QueueMetricsService;

/**
 * Automates Card applications:
 * - Debit cards: auto-approve with default expiry (service defaults to now + 5 years if not provided).
 * - Credit cards:
 *    * If user has SALARY_CORPORATE account AND joined balance > 200,000 => limit 100,000
 *    * If user has NO SALARY_CORPORATE and joined balance < 100,000 => limit 50,000
 *    * If joined balance < 50,000 => reject
 *    * Otherwise conservative: limit 50,000 (non-salary, joined >= 100,000)
 */
@Service
public class CardWorkflow {

    private static final Logger log = LoggerFactory.getLogger(CardWorkflow.class);

    private static final String ACCOUNT_TYPE_SALARY = "SALARY_CORPORATE";
    private static final String TYPE_DEBIT = "DEBIT";
    private static final String TYPE_CREDIT = "CREDIT";

    private static final long BALANCE_200K = 200_000L;
    private static final long BALANCE_100K = 100_000L;
    private static final long BALANCE_50K  = 50_000L;

    private static final double LIMIT_CREDIT_CORPORATE = 100_000d;
    private static final double LIMIT_CREDIT_NON_SALARY = 50_000d;

    private final CardAdminClient cardClient;
    private final AccountAdminClient accountClient;
    private final AgentStateService agentStateService;
    private final QueueMetricsService queueMetricsService;
    private final AiReasoner aiReasoner;

    public CardWorkflow(CardAdminClient cardClient,
                        AccountAdminClient accountClient,
                        AgentStateService agentStateService,
                        QueueMetricsService queueMetricsService,
                        AiReasoner aiReasoner) {
        this.cardClient = cardClient;
        this.accountClient = accountClient;
        this.agentStateService = agentStateService;
        this.queueMetricsService = queueMetricsService;
        this.aiReasoner = aiReasoner;
    }

    public void process() {
        List<CardApplicationResponse> apps;
        try {
            apps = cardClient.listApplicationsByStatus("SUBMITTED");
        } catch (Exception ex) {
            log.error("Cards: failed to fetch SUBMITTED applications: {}", ex.getMessage());
            return;
        }
        if (apps == null || apps.isEmpty()) {
            log.debug("Cards: no SUBMITTED applications");
            queueMetricsService.setQueueSize("cards", 0);
            return;
        }

        int pending = 0;
        for (CardApplicationResponse app : apps) {
            if (!"SUBMITTED".equalsIgnoreCase(s(app.getStatus()))) continue;
            pending++;

            Decision d = decide(app);
            if (agentStateService.getMode() == AgentMode.DRY_RUN) {
                log.info("Cards[DRY_RUN]: appId={} user={} type={} decision={} limit={} comment='{}'",
                        app.getApplicationId(), app.getUserId(), s(app.getType()), d.action, d.approvedLimit, d.comment);
                continue;
            }
            if (agentStateService.getMode() == AgentMode.OFF) {
                log.debug("Cards: Agent mode OFF; skipping reviews");
                break;
            }

            try {
                if (d.action == Action.APPROVE) {
                    ReviewCardApplicationRequest body = new ReviewCardApplicationRequest();
                    body.setDecision(ReviewCardApplicationRequest.Decision.APPROVED);
                    body.setReviewerId("agent");
                    body.setApprovedLimit(d.approvedLimit);
                    // expiry left null to use default 5 years
                    body.setAdminComment(d.comment);
                    var resp = cardClient.reviewApplication(app.getApplicationId(), body);
                    log.info("Cards: approved app {} for user {} -> status={} limit={}",
                            app.getApplicationId(), app.getUserId(), s(resp.getStatus()), d.approvedLimit);
                } else {
                    ReviewCardApplicationRequest body = new ReviewCardApplicationRequest();
                    body.setDecision(ReviewCardApplicationRequest.Decision.REJECTED);
                    body.setReviewerId("agent");
                    body.setAdminComment(d.comment);
                    var resp = cardClient.reviewApplication(app.getApplicationId(), body);
                    log.info("Cards: rejected app {} for user {} -> status={} reason='{}'",
                            app.getApplicationId(), app.getUserId(), s(resp.getStatus()), d.comment);
                }
            } catch (Exception ex) {
                log.error("Cards: review failed for app {}: {}", app.getApplicationId(), ex.getMessage());
            }
        }
        queueMetricsService.setQueueSize("cards", pending);
    }

    private Decision decide(CardApplicationResponse app) {
        String type = s(app.getType());
        if (TYPE_DEBIT.equalsIgnoreCase(type)) {
            return approve(null, "Auto-approved DEBIT card with default expiry.");
        }
        if (!TYPE_CREDIT.equalsIgnoreCase(type)) {
            return reject("Unknown card type. Rejecting conservatively.");
        }

        List<AccountResponse> accounts = null;
        try {
            accounts = accountClient.getAccountsByUser(app.getUserId());
        } catch (Exception ex) {
            String msg = "Could not retrieve accounts to evaluate credit card. Rejecting conservatively.";
            log.warn("Cards: {} user={} error={}", msg, app.getUserId(), ex.getMessage());
            return reject(msg);
        }
        long joined = sumBalances(accounts);
        boolean hasSalary = hasCorporateAccount(accounts);

        if (joined < BALANCE_50K) {
            String msg = String.format(Locale.US, "Joined balances %,d below 50,000. Rejecting credit card request.", joined);
            try {
                var rr = aiReasoner.evaluate("CARD_CREDIT_LIMIT_POLICY", Map.of(
                        "hasSalaryAccount", hasSalary,
                        "joinedBalance", joined,
                        "requestedType", "CREDIT"
                ));
                if (rr != null && rr.getReason() != null && !rr.getReason().isBlank()) {
                    msg = msg + " " + rr.getReason();
                }
            } catch (Exception ignored) { }
            return reject(msg);
        }
        if (hasSalary && joined > BALANCE_200K) {
            return approve(LIMIT_CREDIT_CORPORATE, String.format(Locale.US,
                    "Auto-approved credit card (salary/corporate present; joined balance %,d > 200,000). Limit %,.0f.",
                    joined, LIMIT_CREDIT_CORPORATE));
        }
        if (!hasSalary && joined < BALANCE_100K) {
            return approve(LIMIT_CREDIT_NON_SALARY, String.format(Locale.US,
                    "Auto-approved credit card (no salary; joined balance %,d < 100,000). Limit %,.0f.",
                    joined, LIMIT_CREDIT_NON_SALARY));
        }

        // Conservative default for non-salary with >= 100k
        return approve(LIMIT_CREDIT_NON_SALARY, String.format(Locale.US,
                "Auto-approved credit card (non-salary profile). Conservative limit %,.0f.", LIMIT_CREDIT_NON_SALARY));
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

    private String s(Object o) { return o == null ? null : String.valueOf(o); }

    private Decision approve(Double limit, String comment) { return new Decision(Action.APPROVE, limit, comment); }
    private Decision reject(String comment) { return new Decision(Action.REJECT, null, comment); }

    private enum Action { APPROVE, REJECT }
    private record Decision(Action action, Double approvedLimit, String comment) {}
}
