package com.bank.aiorchestrator.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * LangChain4j + Ollama implementation of AiReasoner.
 * - Expects the model to return a strict JSON object:
 *   { "decision": "APPROVE|REJECT|INCONCLUSIVE", "reason": "..." }
 * - If parsing fails, falls back to INCONCLUSIVE with a diagnostic reason.
 * - Deterministic rule engines in workflows should remain the source of truth;
 *   AI augments unstructured validation and reason generation.
 */
public class LlmAiReasoner implements AiReasoner {

    private static final Logger log = LoggerFactory.getLogger(LlmAiReasoner.class);

    private final ChatLanguageModel model;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmAiReasoner(ChatLanguageModel model) {
        this.model = model;
    }

    @Override
    public ReasoningResult evaluate(String task, Map<String, Object> inputs) {
        try {
            String inputsJson = mapper.writeValueAsString(inputs == null ? Map.of() : inputs);

            String prompt = """
                You are an AI assistant helping a bank admin to review requests.
                Task: %s

                Inputs (JSON):
                %s

                Carefully analyze the inputs and decide one of:
                  - APPROVE: when evidence and policy checks support approval.
                  - REJECT: when there are inconsistencies, missing or invalid evidence, or policy limits exceeded.
                  - INCONCLUSIVE: when you cannot be confident either way.

                Output MUST be a strict JSON object only (no extra text), exactly with these fields:
                {
                  "decision": "APPROVE" | "REJECT" | "INCONCLUSIVE",
                  "reason": "Short human-friendly justification explaining the decision"
                }

                Rules of thumb:
                - Prefer REJECT if documents/inputs contradict the request, or key fields are missing.
                - Prefer INCONCLUSIVE if evidence is insufficient or unclear.
                - Do not fabricate details. Stay concise (<= 2 sentences).
                """.formatted(task, inputsJson);

            String raw = model.generate(prompt);
            Map<String, Object> parsed = tryParseJsonObject(raw);
            if (parsed == null) {
                // Try extracting a JSON block if model added prose
                String jsonBlock = extractFirstJsonObject(raw);
                if (jsonBlock != null) {
                    parsed = tryParseJsonObject(jsonBlock);
                }
            }

            if (parsed == null) {
                log.warn("LLM output not parseable as JSON: {}", truncate(raw, 500));
                return new ReasoningResult(Decision.INCONCLUSIVE, "LLM output not parseable.");
            }

            Decision decision = parseDecision(parsed.get("decision"));
            String reason = toStringSafe(parsed.get("reason"));
            if (decision == null) decision = Decision.INCONCLUSIVE;
            if (isBlank(reason)) reason = "No reason provided by LLM.";

            return new ReasoningResult(decision, reason);

        } catch (Exception e) {
            log.warn("LlmAiReasoner error: {}", e.toString());
            return new ReasoningResult(Decision.INCONCLUSIVE, "LLM error: " + e.getMessage());
        }
    }

    private Map<String, Object> tryParseJsonObject(String s) {
        if (isBlank(s)) return null;
        try {
            return mapper.readValue(s, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractFirstJsonObject(String text) {
        if (isBlank(text)) return null;
        // Strip Markdown code fences if present
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) {
                t = t.substring(firstNewline + 1);
                int fence = t.indexOf("```");
                if (fence >= 0) t = t.substring(0, fence);
            }
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1).trim();
        }
        return null;
    }

    private Decision parseDecision(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "APPROVE" -> Decision.APPROVE;
            case "REJECT" -> Decision.REJECT;
            case "INCONCLUSIVE" -> Decision.INCONCLUSIVE;
            default -> null;
        };
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String toStringSafe(Object o) { return o == null ? null : String.valueOf(o); }
    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // Example formatter kept to hint models for date formats if provided in inputs
    @SuppressWarnings("unused")
    private String isoDate(java.time.LocalDate d) {
        return d == null ? null : d.format(DateTimeFormatter.ISO_DATE);
    }
}
