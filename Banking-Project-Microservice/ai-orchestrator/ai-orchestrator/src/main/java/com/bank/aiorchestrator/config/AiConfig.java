package com.bank.aiorchestrator.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bank.aiorchestrator.ai.AiReasoner;
import com.bank.aiorchestrator.ai.LlmAiReasoner;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

/**
 * AI wiring:
 * - When orchestrator.ai.provider=ollama, creates an Ollama-backed ChatLanguageModel
 *   and exposes a LangChain4j-based AiReasoner (LlmAiReasoner).
 * - When provider=noop (default), NoopAiReasoner is active via its own conditional annotation.
 */
@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnProperty(name = "orchestrator.ai.provider", havingValue = "ollama")
    public ChatLanguageModel ollamaChatLanguageModel(OrchestratorProperties props) {
        OrchestratorProperties.Ai.Ollama c = props.getAi().getOllama();
        return OllamaChatModel.builder()
                .baseUrl(c.getBaseUrl())
                .modelName(c.getModel())
                .temperature(c.getTemperature())
                .timeout(Duration.ofSeconds(c.getTimeoutSec()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "orchestrator.ai.provider", havingValue = "ollama")
    public AiReasoner aiReasoner(ChatLanguageModel model) {
        return new LlmAiReasoner(model);
    }
}
