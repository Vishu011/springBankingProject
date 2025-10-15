package com.bank.aiorchestrator.web.dto;

import com.bank.aiorchestrator.model.AgentMode;

import java.util.HashMap;
import java.util.Map;

public class AgentStatusResponse {
    private boolean enabled;
    private AgentMode mode;
    private Workflows workflows = new Workflows();
    private boolean pollingEnabled;
    private long pollingIntervalMs;
    private String lastRunAt; // ISO-8601 string
    private Map<String, Integer> queues = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AgentMode getMode() {
        return mode;
    }

    public void setMode(AgentMode mode) {
        this.mode = mode;
    }

    public Workflows getWorkflows() {
        return workflows;
    }

    public void setWorkflows(Workflows workflows) {
        this.workflows = workflows;
    }

    public boolean isPollingEnabled() {
        return pollingEnabled;
    }

    public void setPollingEnabled(boolean pollingEnabled) {
        this.pollingEnabled = pollingEnabled;
    }

    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public String getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(String lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Map<String, Integer> getQueues() {
        return queues;
    }

    public void setQueues(Map<String, Integer> queues) {
        this.queues = queues;
    }

    public static class Workflows {
        private boolean kyc;
        private boolean loans;
        private boolean salary;
        private boolean cards;
        private boolean selfService;

        public boolean isKyc() {
            return kyc;
        }

        public void setKyc(boolean kyc) {
            this.kyc = kyc;
        }

        public boolean isLoans() {
            return loans;
        }

        public void setLoans(boolean loans) {
            this.loans = loans;
        }

        public boolean isSalary() {
            return salary;
        }

        public void setSalary(boolean salary) {
            this.salary = salary;
        }

        public boolean isCards() {
            return cards;
        }

        public void setCards(boolean cards) {
            this.cards = cards;
        }

        public boolean isSelfService() {
            return selfService;
        }

        public void setSelfService(boolean selfService) {
            this.selfService = selfService;
        }
    }
}
