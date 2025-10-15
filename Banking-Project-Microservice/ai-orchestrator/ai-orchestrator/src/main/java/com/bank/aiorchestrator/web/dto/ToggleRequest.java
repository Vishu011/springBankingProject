package com.bank.aiorchestrator.web.dto;

import com.bank.aiorchestrator.model.AgentMode;

public class ToggleRequest {
    private Boolean enabled;
    private AgentMode mode;
    private Workflows workflows;
    private Boolean pollingEnabled;
    private Long pollingIntervalMs;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
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

    public Boolean getPollingEnabled() {
        return pollingEnabled;
    }

    public void setPollingEnabled(Boolean pollingEnabled) {
        this.pollingEnabled = pollingEnabled;
    }

    public Long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(Long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public static class Workflows {
        private Boolean kyc;
        private Boolean loans;
        private Boolean salary;
        private Boolean cards;
        private Boolean selfService;

        public Boolean getKyc() {
            return kyc;
        }

        public void setKyc(Boolean kyc) {
            this.kyc = kyc;
        }

        public Boolean getLoans() {
            return loans;
        }

        public void setLoans(Boolean loans) {
            this.loans = loans;
        }

        public Boolean getSalary() {
            return salary;
        }

        public void setSalary(Boolean salary) {
            this.salary = salary;
        }

        public Boolean getCards() {
            return cards;
        }

        public void setCards(Boolean cards) {
            this.cards = cards;
        }

        public Boolean getSelfService() {
            return selfService;
        }

        public void setSelfService(Boolean selfService) {
            this.selfService = selfService;
        }
    }
}
