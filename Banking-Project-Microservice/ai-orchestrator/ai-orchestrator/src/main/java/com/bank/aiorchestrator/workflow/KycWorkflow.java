package com.bank.aiorchestrator.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.bank.aiorchestrator.ai.AiReasoner;
import com.bank.aiorchestrator.ai.OcrService;
import com.bank.aiorchestrator.integrations.user.KycAdminClient;
import com.bank.aiorchestrator.integrations.user.dto.KycApplicationDto;
import com.bank.aiorchestrator.integrations.user.dto.KycReviewStatus;
import com.bank.aiorchestrator.model.AgentMode;
import com.bank.aiorchestrator.service.AgentStateService;
import com.bank.aiorchestrator.service.QueueMetricsService;

/**
 * Automates the KYC admin review workflow.
 * Logic v1:
 *  - Fetch SUBMITTED applications
 *  - Validate basic structure (Aadhaar/PAN formats, address present, docs present)
 *  - Approve if all checks pass; else reject with reason(s)
 *  - In DRY_RUN, only log decisions without invoking review endpoint
 */
@Service
public class KycWorkflow {

    private static final Logger log = LoggerFactory.getLogger(KycWorkflow.class);

    private static final Pattern AADHAAR_REGEX = Pattern.compile("^\\d{12}$");
    private static final Pattern PAN_REGEX = Pattern.compile("^[A-Z]{5}\\d{4}[A-Z]$");

    private final KycAdminClient kycAdminClient;
    private final AgentStateService agentStateService;
    private final QueueMetricsService queueMetricsService;
    private final OcrService ocrService;
    private final AiReasoner aiReasoner;

    public KycWorkflow(KycAdminClient kycAdminClient,
                       AgentStateService agentStateService,
                       QueueMetricsService queueMetricsService,
                       @Qualifier("tikaOcrService") OcrService ocrService,
                       AiReasoner aiReasoner) {
        this.kycAdminClient = kycAdminClient;
        this.agentStateService = agentStateService;
        this.queueMetricsService = queueMetricsService;
        this.ocrService = ocrService;
        this.aiReasoner = aiReasoner;
    }

    public void process() {
        try {
            List<KycApplicationDto> submitted = kycAdminClient.listByStatus(KycReviewStatus.SUBMITTED);
            queueMetricsService.setQueueSize("kyc", submitted == null ? 0 : submitted.size());
            if (submitted == null || submitted.isEmpty()) {
                log.debug("KYC: no SUBMITTED applications to process");
                return;
            }

            for (KycApplicationDto app : submitted) {
                Decision d = decide(app);
                if (agentStateService.getMode() == AgentMode.DRY_RUN) {
                    log.info("KYC[DRY_RUN]: {} -> decision={} comment={}", app.getApplicationId(), d.status, d.comment);
                    continue;
                }
                if (agentStateService.getMode() == AgentMode.OFF) {
                    log.debug("KYC: Agent mode OFF; skipping reviews");
                    break;
                }

                try {
                    var updated = kycAdminClient.review(app.getApplicationId(), d.status, d.comment);
                    log.info("KYC: reviewed application {} -> {} (comment='{}')", app.getApplicationId(), updated.getReviewStatus(), d.comment);
                } catch (Exception ex) {
                    log.error("KYC: failed to review application {}: {}", app.getApplicationId(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("KYC: processing failed: {}", e.getMessage(), e);
        }
    }

    private Decision decide(KycApplicationDto app) {
        List<String> issues = new ArrayList<>();

        // Aadhaar
        if (isBlank(app.getAadharNumber()) || !AADHAAR_REGEX.matcher(app.getAadharNumber().trim()).matches()) {
            issues.add("Invalid Aadhaar format (must be 12 digits).");
        }

        // PAN
        if (isBlank(app.getPanNumber()) || !PAN_REGEX.matcher(app.getPanNumber().trim()).matches()) {
            issues.add("Invalid PAN format (ABCDE1234F).");
        }

        // Address
        if (isBlank(app.getAddressLine1()) || isBlank(app.getCity()) || isBlank(app.getState()) || isBlank(app.getPostalCode())) {
            issues.add("Incomplete address details.");
        }

        // Documents
        if (app.getDocumentPaths() == null || app.getDocumentPaths().isEmpty()) {
            issues.add("No supporting documents uploaded.");
        }

        if (issues.isEmpty()) {
            // Corroborate PAN/Aadhaar/address in documents using OCR, fall back to AI if ambiguous.
            String docText = extractAllDocText(app);
            boolean panInDoc = containsIgnoreCase(docText, s(app.getPanNumber()));
            String aad = s(app.getAadharNumber());
            boolean aadLast4InDoc = aad != null && aad.length() >= 4 && containsIgnoreCase(docText, aad.substring(aad.length() - 4));
            boolean addressInDoc = containsIgnoreCase(docText, s(app.getAddressLine1()))
                    || containsIgnoreCase(docText, s(app.getCity()))
                    || containsIgnoreCase(docText, s(app.getPostalCode()));
            if (panInDoc || aadLast4InDoc || addressInDoc) {
                return new Decision(KycReviewStatus.APPROVED, "Auto-approved by AI agent (documents corroborate identity details).");
            }
            // Consult AI before rejecting; provide structured inputs.
            Map<String, Object> inputs = Map.of(
                    "aadharNumber", s(app.getAadharNumber()),
                    "panNumber", s(app.getPanNumber()),
                    "addressLine1", s(app.getAddressLine1()),
                    "city", s(app.getCity()),
                    "state", s(app.getState()),
                    "postalCode", s(app.getPostalCode()),
                    "docText", safeTruncate(docText, 4000)
            );
            var rr = aiReasoner.evaluate("KYC_VALIDATION", inputs);
            if (rr.getDecision() == AiReasoner.Decision.APPROVE) {
                return new Decision(KycReviewStatus.APPROVED, "AI-approved KYC. " + rr.getReason());
            }
            if (rr.getDecision() == AiReasoner.Decision.REJECT) {
                return new Decision(KycReviewStatus.REJECTED, rr.getReason());
            }
            return new Decision(KycReviewStatus.REJECTED, "Documents do not corroborate KYC details.");
        }
        return new Decision(KycReviewStatus.REJECTED, "Auto-rejected: " + String.join(" ", issues));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // OCR helpers
    private String extractAllDocText(KycApplicationDto app) {
        if (app.getDocumentPaths() == null || app.getDocumentPaths().isEmpty()) return "";
        Map<String, byte[]> docs = new HashMap<>();
        for (String rel : app.getDocumentPaths()) {
            try {
                var resp = kycAdminClient.downloadDocument(app.getApplicationId(), rel);
                if (resp != null && resp.body() != null) {
                    docs.put(rel, resp.body().asInputStream().readAllBytes());
                }
            } catch (Exception ignored) {}
        }
        Map<String, String> texts = ocrService.extractTextFromBytes(docs);
        StringBuilder sb = new StringBuilder();
        if (texts != null) {
            for (String t : texts.values()) {
                if (t != null) sb.append(' ').append(t);
            }
        }
        return sb.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) return false;
        return haystack.toLowerCase(java.util.Locale.ROOT).contains(needle.toLowerCase(java.util.Locale.ROOT));
    }

    private String safeTruncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String s(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private record Decision(KycReviewStatus status, String comment) {}
}
