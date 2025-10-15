package com.bank.aiorchestrator.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.bank.aiorchestrator.workflow.CardWorkflow;
import com.bank.aiorchestrator.workflow.KycWorkflow;
import com.bank.aiorchestrator.workflow.LoanWorkflow;
import com.bank.aiorchestrator.workflow.SalaryWorkflow;
import com.bank.aiorchestrator.workflow.SelfServiceWorkflow;

/**
 * Stub orchestrator coordinator. In later phases this will:
 * - Poll pending queues (KYC/Loans/Salary/Cards/SelfService)
 * - Run rule engine + LLM validation
 * - Approve/Reject via existing admin endpoints
 * - Record audit entries
 */
@Service
public class OrchestratorService {

    private final AtomicReference<Instant> lastRunAt = new AtomicReference<>(null);

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final AgentStateService agentStateService;
    private final KycWorkflow kycWorkflow;
    private final LoanWorkflow loanWorkflow;
    private final CardWorkflow cardWorkflow;
    private final SalaryWorkflow salaryWorkflow;
    private final SelfServiceWorkflow selfServiceWorkflow;
    private final QueueMetricsService queueMetricsService;

    public OrchestratorService(AgentStateService agentStateService, KycWorkflow kycWorkflow,
                               LoanWorkflow loanWorkflow, CardWorkflow cardWorkflow,
                               SalaryWorkflow salaryWorkflow, SelfServiceWorkflow selfServiceWorkflow,
                               QueueMetricsService queueMetricsService) {
        this.agentStateService = agentStateService;
        this.kycWorkflow = kycWorkflow;
        this.loanWorkflow = loanWorkflow;
        this.cardWorkflow = cardWorkflow;
        this.salaryWorkflow = salaryWorkflow;
        this.selfServiceWorkflow = selfServiceWorkflow;
        this.queueMetricsService = queueMetricsService;
        this.queueMetricsService.ensureDefaults();
    }

    public void runNow() {
        lastRunAt.set(Instant.now());
        // Dispatch enabled workflows
        try {
            if (agentStateService.kycEnabled()) {
                kycWorkflow.process();
            }
            if (agentStateService.loansEnabled()) {
                loanWorkflow.process();
            }
            if (agentStateService.cardsEnabled()) {
                cardWorkflow.process();
            }
            if (agentStateService.salaryEnabled()) {
                salaryWorkflow.process();
            }
            if (agentStateService.selfServiceEnabled()) {
                selfServiceWorkflow.process();
            }
        } catch (Exception e) {
            // swallow to avoid scheduler crash, but log in a real logger impl
            System.err.println("Orchestrator run failed: " + e.getMessage());
        }
    }

    public String getLastRunAtIso() {
        Instant v = lastRunAt.get();
        if (v == null) return null;
        return ISO.format(v.atOffset(ZoneOffset.UTC));
    }

    public Instant getLastRunInstant() {
        return lastRunAt.get();
    }

    public Map<String, Integer> getCurrentQueuesSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(queueMetricsService.snapshot()));
    }

}
