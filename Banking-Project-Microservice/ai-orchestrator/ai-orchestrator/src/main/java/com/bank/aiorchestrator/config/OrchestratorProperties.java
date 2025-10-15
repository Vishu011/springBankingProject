package com.bank.aiorchestrator.config;

import com.bank.aiorchestrator.model.AgentMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator")
public class OrchestratorProperties {

    private Agent agent = new Agent();
    private Integrations integrations = new Integrations();
    private Ai ai = new Ai();

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Integrations getIntegrations() {
        return integrations;
    }

    public void setIntegrations(Integrations integrations) {
        this.integrations = integrations;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    // Nested classes

    public static class Agent {
        private boolean enabled = false;
        private AgentMode mode = AgentMode.OFF;
        private Workflows workflows = new Workflows();
        private Polling polling = new Polling();

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

        public Polling getPolling() {
            return polling;
        }

        public void setPolling(Polling polling) {
            this.polling = polling;
        }
    }

    public static class Workflows {
        private boolean kyc = true;
        private boolean loans = true;
        private boolean salary = true;
        private boolean cards = true;
        private boolean selfService = true;

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

    public static class Polling {
        private boolean enabled = true;
        private long intervalMs = 30000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }
    }

    public static class Integrations {
        private String gatewayBaseUrl = "http://localhost:8088";
        private String userServiceBaseUrl = "http://localhost:8081";
        private String accountServiceBaseUrl = "http://localhost:8082";
        private String loanServiceBaseUrl = "http://localhost:8083";
        private String creditCardServiceBaseUrl = "http://localhost:8084";
        private String selfServiceBaseUrl = "http://localhost:8085";
        private String notificationServiceBaseUrl = "http://localhost:8086";

        public String getGatewayBaseUrl() {
            return gatewayBaseUrl;
        }

        public void setGatewayBaseUrl(String gatewayBaseUrl) {
            this.gatewayBaseUrl = gatewayBaseUrl;
        }

        public String getUserServiceBaseUrl() {
            return userServiceBaseUrl;
        }

        public void setUserServiceBaseUrl(String userServiceBaseUrl) {
            this.userServiceBaseUrl = userServiceBaseUrl;
        }

        public String getAccountServiceBaseUrl() {
            return accountServiceBaseUrl;
        }

        public void setAccountServiceBaseUrl(String accountServiceBaseUrl) {
            this.accountServiceBaseUrl = accountServiceBaseUrl;
        }

        public String getLoanServiceBaseUrl() {
            return loanServiceBaseUrl;
        }

        public void setLoanServiceBaseUrl(String loanServiceBaseUrl) {
            this.loanServiceBaseUrl = loanServiceBaseUrl;
        }

        public String getCreditCardServiceBaseUrl() {
            return creditCardServiceBaseUrl;
        }

        public void setCreditCardServiceBaseUrl(String creditCardServiceBaseUrl) {
            this.creditCardServiceBaseUrl = creditCardServiceBaseUrl;
        }

        public String getSelfServiceBaseUrl() {
            return selfServiceBaseUrl;
        }

        public void setSelfServiceBaseUrl(String selfServiceBaseUrl) {
            this.selfServiceBaseUrl = selfServiceBaseUrl;
        }

        public String getNotificationServiceBaseUrl() {
            return notificationServiceBaseUrl;
        }

        public void setNotificationServiceBaseUrl(String notificationServiceBaseUrl) {
            this.notificationServiceBaseUrl = notificationServiceBaseUrl;
        }
    }

    // AI configuration
    public static class Ai {
        private String provider = "noop"; // noop | ollama
        private Ollama ollama = new Ollama();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public Ollama getOllama() {
            return ollama;
        }

        public void setOllama(Ollama ollama) {
            this.ollama = ollama;
        }

        public static class Ollama {
            private String baseUrl = "http://localhost:11434";
            private String model = "llama3.1";
            private double temperature = 0.1;
            private int timeoutSec = 60;

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public double getTemperature() {
                return temperature;
            }

            public void setTemperature(double temperature) {
                this.temperature = temperature;
            }

            public int getTimeoutSec() {
                return timeoutSec;
            }

            public void setTimeoutSec(int timeoutSec) {
                this.timeoutSec = timeoutSec;
            }
        }
    }
}
