package com.bank.aiorchestrator.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers orchestrator runs when Agent mode is enabled and polling is on.
 * Uses a short fixed delay and enforces the configured interval at runtime so that
 * interval can be changed via toggles without restarting the app.
 */
@Component
public class AgentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgentScheduler.class);

    private final AgentStateService agentState;
    private final OrchestratorService orchestrator;

    public AgentScheduler(AgentStateService agentState, OrchestratorService orchestrator) {
        this.agentState = agentState;
        this.orchestrator = orchestrator;
    }

    // Runs every 5 seconds, but only triggers when interval elapsed.
    @Scheduled(fixedDelay = 5000L, initialDelay = 5000L)
    public void tick() {
        try {
            if (!agentState.isEnabled() || !agentState.isPollingEnabled()) {
                return;
            }
            long intervalMs = agentState.getPollingIntervalMs();
            Instant last = orchestrator.getLastRunInstant();
            if (last == null || Duration.between(last, Instant.now()).toMillis() >= intervalMs) {
                log.debug("AgentScheduler: triggering orchestrator run (intervalMs={})", intervalMs);
                orchestrator.runNow();
            }
        } catch (Exception ex) {
            log.error("AgentScheduler tick failed: {}", ex.getMessage(), ex);
        }
    }
}
