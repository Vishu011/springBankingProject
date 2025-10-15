package com.bank.aiorchestrator.service;

import com.bank.aiorchestrator.config.OrchestratorProperties;
import com.bank.aiorchestrator.model.AgentMode;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the runtime Agent state (enabled/mode/per-workflow flags).
 * Seeded from OrchestratorProperties at startup, but mutable via /agent/toggle.
 */
@Service
public class AgentStateService {

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicReference<AgentMode> mode = new AtomicReference<>(AgentMode.OFF);

    private final AtomicBoolean wfKyc = new AtomicBoolean(true);
    private final AtomicBoolean wfLoans = new AtomicBoolean(true);
    private final AtomicBoolean wfSalary = new AtomicBoolean(true);
    private final AtomicBoolean wfCards = new AtomicBoolean(true);
    private final AtomicBoolean wfSelfService = new AtomicBoolean(true);

    private final AtomicBoolean pollingEnabled = new AtomicBoolean(true);
    private final AtomicReference<Long> pollingIntervalMs = new AtomicReference<>(30_000L);

    public AgentStateService(OrchestratorProperties props) {
        if (props != null && props.getAgent() != null) {
            enabled.set(props.getAgent().isEnabled());
            mode.set(Objects.requireNonNullElse(props.getAgent().getMode(), AgentMode.OFF));

            if (props.getAgent().getWorkflows() != null) {
                wfKyc.set(props.getAgent().getWorkflows().isKyc());
                wfLoans.set(props.getAgent().getWorkflows().isLoans());
                wfSalary.set(props.getAgent().getWorkflows().isSalary());
                wfCards.set(props.getAgent().getWorkflows().isCards());
                wfSelfService.set(props.getAgent().getWorkflows().isSelfService());
            }
            if (props.getAgent().getPolling() != null) {
                pollingEnabled.set(props.getAgent().getPolling().isEnabled());
                pollingIntervalMs.set(props.getAgent().getPolling().getIntervalMs());
            }
        }
    }

    // Getters
    public boolean isEnabled() { return enabled.get(); }
    public AgentMode getMode() { return mode.get(); }

    public boolean kycEnabled() { return wfKyc.get(); }
    public boolean loansEnabled() { return wfLoans.get(); }
    public boolean salaryEnabled() { return wfSalary.get(); }
    public boolean cardsEnabled() { return wfCards.get(); }
    public boolean selfServiceEnabled() { return wfSelfService.get(); }

    public boolean isPollingEnabled() { return pollingEnabled.get(); }
    public long getPollingIntervalMs() { return pollingIntervalMs.get(); }

    // Mutators
    public void setEnabled(boolean value) { enabled.set(value); }
    public void setMode(AgentMode value) { mode.set(value == null ? AgentMode.OFF : value); }

    public void setKyc(boolean value) { wfKyc.set(value); }
    public void setLoans(boolean value) { wfLoans.set(value); }
    public void setSalary(boolean value) { wfSalary.set(value); }
    public void setCards(boolean value) { wfCards.set(value); }
    public void setSelfService(boolean value) { wfSelfService.set(value); }

    public void setPollingEnabled(boolean value) { pollingEnabled.set(value); }
    public void setPollingIntervalMs(long value) { pollingIntervalMs.set(value); }
}
