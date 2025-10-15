package com.bank.aiorchestrator.ai;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Phase-1 no-op LLM reasoner. Always returns INCONCLUSIVE so that
 * deterministic rule engines decide. Useful to keep the workflow
 * code paths stable before integrating LangChain4j + Ollama.
 */
@Service
@ConditionalOnProperty(name = "orchestrator.ai.provider", havingValue = "noop", matchIfMissing = true)
public class NoopAiReasoner implements AiReasoner {

    private static final Logger log = LoggerFactory.getLogger(NoopAiReasoner.class);

    @Override
    public ReasoningResult evaluate(String task, Map<String, Object> inputs) {
        log.debug("NoopAiReasoner.evaluate task={} inputsKeys={}", task, inputs == null ? 0 : inputs.keySet());
        return new ReasoningResult(Decision.INCONCLUSIVE, "LLM disabled (no-op). Deterministic rules should decide.");
    }
}
