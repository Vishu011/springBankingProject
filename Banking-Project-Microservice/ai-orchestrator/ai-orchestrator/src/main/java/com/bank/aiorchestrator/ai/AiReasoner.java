package com.bank.aiorchestrator.ai;

import java.util.Map;

/**
 * Abstraction for LLM-based reasoning. In Phase 1 we provide a no-op implementation.
 * Later, integrate LangChain4j + Ollama and return structured, explainable results.
 */
public interface AiReasoner {

    enum Decision {
        APPROVE,
        REJECT,
        INCONCLUSIVE
    }

    class ReasoningResult {
        private Decision decision;
        private String reason;

        public ReasoningResult() {}

        public ReasoningResult(Decision decision, String reason) {
            this.decision = decision;
            this.reason = reason;
        }

        public Decision getDecision() {
            return decision;
        }

        public void setDecision(Decision decision) {
            this.decision = decision;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * Generic classification/verification hook.
     * task: e.g. "KYC_VALIDATION", "SALARY_EMPLOYMENT_CHECK", "SELF_SERVICE_CHANGE_CHECK"
     * inputs: OCR text snippets, structured fields, heuristics, etc.
     *
     * For Phase 1, return INCONCLUSIVE to allow deterministic rules to decide.
     */
    ReasoningResult evaluate(String task, Map<String, Object> inputs);
}
