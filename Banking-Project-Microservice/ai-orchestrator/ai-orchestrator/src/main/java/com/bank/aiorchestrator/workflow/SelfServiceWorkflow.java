package com.bank.aiorchestrator.workflow;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.bank.aiorchestrator.ai.OcrService;
import com.bank.aiorchestrator.ai.AiReasoner;
import com.bank.aiorchestrator.integrations.selfservice.SelfServiceAdminClient;
import com.bank.aiorchestrator.integrations.selfservice.dto.SelfServiceAdminDecisionRequest;
import com.bank.aiorchestrator.integrations.selfservice.dto.SelfServiceRequest;
import com.bank.aiorchestrator.integrations.user.UserProfileClient;
import com.bank.aiorchestrator.integrations.user.dto.UserProfileResponse;
import com.bank.aiorchestrator.model.AgentMode;
import com.bank.aiorchestrator.service.AgentStateService;
import com.bank.aiorchestrator.service.QueueMetricsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Automates Self-Service profile change requests (NAME/DOB/ADDRESS).
 * Phase 1: Deterministic checks only (documents present, field changes valid).
 * Phase 2+: Integrate OCR/LLM to parse and validate docs against requested changes.
 */
@Service
public class SelfServiceWorkflow {

    private static final Logger log = LoggerFactory.getLogger(SelfServiceWorkflow.class);

    private static final String TYPE_NAME = "NAME_CHANGE";
    private static final String TYPE_DOB = "DOB_CHANGE";
    private static final String TYPE_ADDRESS = "ADDRESS_CHANGE";

    private final SelfServiceAdminClient selfServiceClient;
    private final UserProfileClient userProfileClient;
    private final AgentStateService agentStateService;
    private final QueueMetricsService queueMetricsService;
    private final OcrService ocrService;
    private final AiReasoner aiReasoner;
    private final ObjectMapper mapper = new ObjectMapper();

    public SelfServiceWorkflow(SelfServiceAdminClient selfServiceClient,
                               UserProfileClient userProfileClient,
                               AgentStateService agentStateService,
                               QueueMetricsService queueMetricsService,
                               @Qualifier("tikaOcrService") OcrService ocrService,
                               AiReasoner aiReasoner) {
        this.selfServiceClient = selfServiceClient;
        this.userProfileClient = userProfileClient;
        this.agentStateService = agentStateService;
        this.queueMetricsService = queueMetricsService;
        this.ocrService = ocrService;
        this.aiReasoner = aiReasoner;
    }

    public void process() {
        List<SelfServiceRequest> reqs;
        try {
            reqs = selfServiceClient.listByStatus("SUBMITTED");
        } catch (Exception ex) {
            log.error("SelfService: failed to fetch SUBMITTED requests: {}", ex.getMessage());
            return;
        }
        if (reqs == null || reqs.isEmpty()) {
            log.debug("SelfService: no SUBMITTED requests");
            queueMetricsService.setQueueSize("selfService", 0);
            return;
        }

        // Update queue size immediately after fetch to reflect pending items quickly
        int pendingBefore = 0;
        for (SelfServiceRequest r : reqs) {
            if ("SUBMITTED".equalsIgnoreCase(s(r.getStatus()))) pendingBefore++;
        }
        queueMetricsService.setQueueSize("selfService", pendingBefore);

        int pending = 0;
        for (SelfServiceRequest r : reqs) {
            if (!"SUBMITTED".equalsIgnoreCase(s(r.getStatus()))) continue;
            pending++;

            Decision d;
            try {
                d = decide(r);
            } catch (Exception ex) {
                log.error("SelfService: decide failed for {}: {}", r.getRequestId(), ex.getMessage(), ex);
                continue;
            }
            if (agentStateService.getMode() == AgentMode.DRY_RUN) {
                log.info("SelfService[DRY_RUN]: reqId={} user={} type={} decision={} comment='{}'",
                        r.getRequestId(), r.getUserId(), s(r.getType()), d.decision, d.comment);
                continue;
            }
            if (agentStateService.getMode() == AgentMode.OFF) {
                log.debug("SelfService: Agent mode OFF; skipping reviews");
                break;
            }

            // Dependency failure or other condition asked us to defer (do not change status)
            if ("DEFER".equalsIgnoreCase(d.decision)) {
                log.warn("SelfService: deferring {} for user {} -> reason='{}'",
                        r.getRequestId(), r.getUserId(), d.comment);
                continue;
            }

            try {
                if ("APPROVED".equalsIgnoreCase(d.decision)) {
                    var resp = selfServiceClient.approve(r.getRequestId(),
                            new SelfServiceAdminDecisionRequest(d.comment, "agent"));
                    log.info("SelfService: approved {} for user {} -> status={}",
                            r.getRequestId(), r.getUserId(), s(resp.getStatus()));
                } else {
                    var resp = selfServiceClient.reject(r.getRequestId(),
                            new SelfServiceAdminDecisionRequest(d.comment, "agent"));
                    log.info("SelfService: rejected {} for user {} -> status={} reason='{}'",
                            r.getRequestId(), r.getUserId(), s(resp.getStatus()), d.comment);
                }
            } catch (Exception ex) {
                log.error("SelfService: review failed for {}: {}", r.getRequestId(), ex.getMessage());
            }
        }
        queueMetricsService.setQueueSize("selfService", pending);
    }

    private Decision decide(SelfServiceRequest r) {
        // Basic document presence check (Phase 1 requirement)
        boolean hasDocs = r.getDocuments() != null && !r.getDocuments().isEmpty();
        if (!hasDocs) {
            return reject("No supporting documents uploaded for requested change.");
        }

        // Parse JSON payload
        Map<String, Object> payload = null;
        try {
            if (r.getPayloadJson() != null && !r.getPayloadJson().isBlank()) {
                payload = mapper.readValue(r.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ex) {
            return reject("Invalid payload JSON. " + ex.getMessage());
        }
        if (payload == null || payload.isEmpty()) {
            return reject("Empty or missing payload for requested change.");
        }

        // Fetch current user profile to compare
        UserProfileResponse profile;
        try {
            profile = userProfileClient.getUserById(r.getUserId());
        } catch (Exception ex) {
            return new Decision("DEFER", "Unable to fetch current profile for comparison. " + ex.getMessage());
        }
        if (profile == null) {
            return new Decision("DEFER", "User profile not found for id " + r.getUserId());
        }

        String docText = extractAllDocText(r);
        String type = s(r.getType());
        if (TYPE_NAME.equalsIgnoreCase(type)) {
            Map nameMap = null;
            Object nm = payload.get("name");
            if (nm instanceof Map<?, ?> m) {
                nameMap = (Map<?, ?>) nm;
            }
            String newFirst = str(nameMap != null ? nameMap.get("firstName") : payload.get("firstName"));
            String newMiddle = str(nameMap != null ? nameMap.get("middleName") : payload.get("middleName"));
            String newLast = str(nameMap != null ? nameMap.get("lastName") : payload.get("lastName"));
            if (isBlank(newFirst) && isBlank(newLast)) {
                return reject("Name change payload must include firstName and/or lastName.");
            }
            boolean changed = !eqIgnoreNull(profile.getFirstName(), newFirst)
                    || !eqIgnoreNull(profile.getMiddleName(), newMiddle)
                    || !eqIgnoreNull(profile.getLastName(), newLast);
            if (!changed) {
                return reject("Requested name is identical to current record.");
            }
            // Phase 1: approve with document presence; Phase 2+: validate docs via OCR/LLM
            boolean nameInDocs = (!isBlank(newFirst) && containsIgnoreCase(docText, newFirst))
                    || (!isBlank(newLast) && containsIgnoreCase(docText, newLast));
            if (!nameInDocs) {
                // Consult AI before hard-rejecting
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("type", "NAME_CHANGE");
                inputs.put("currentFirst", nz(profile.getFirstName()));
                inputs.put("currentMiddle", nz(profile.getMiddleName()));
                inputs.put("currentLast", nz(profile.getLastName()));
                inputs.put("newFirst", nz(newFirst));
                inputs.put("newMiddle", nz(newMiddle));
                inputs.put("newLast", nz(newLast));
                inputs.put("docText", nz(safeTruncate(docText, 4000)));
                var rr = safeEval("SELF_SERVICE_NAME_CHECK", inputs);
                if (rr.getDecision() == AiReasoner.Decision.APPROVE) {
                    return approve("AI-approved name change. " + rr.getReason());
                }
                if (rr.getDecision() == AiReasoner.Decision.REJECT) {
                    return reject(rr.getReason());
                }
                return reject("Documents do not appear to contain the requested name.");
            }
            return approve(String.format(Locale.US,
                    "Auto-approved name change to '%s %s %s' based on provided documents.",
                    nz(newFirst), nz(newMiddle), nz(newLast)));
        }

        if (TYPE_DOB.equalsIgnoreCase(type)) {
            String dobStr = str(payload.containsKey("dateOfBirth") ? payload.get("dateOfBirth") : payload.get("dob"));
            if (isBlank(dobStr)) {
                return reject("DOB change payload must include 'dateOfBirth' (YYYY-MM-DD).");
            }
            LocalDate newDob;
            try {
                newDob = LocalDate.parse(dobStr);
            } catch (Exception ex) {
                return reject("Invalid dateOfBirth format. Expected YYYY-MM-DD.");
            }
            if (Objects.equals(profile.getDateOfBirth(), newDob)) {
                return reject("Requested dateOfBirth matches current record.");
            }
            if (!docHasDate(docText, newDob)) {
                // Consult AI before hard-rejecting
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("type", "DOB_CHANGE");
                inputs.put("currentDob", String.valueOf(profile.getDateOfBirth()));
                inputs.put("newDob", String.valueOf(newDob));
                inputs.put("docText", nz(safeTruncate(docText, 4000)));
                var rr = safeEval("SELF_SERVICE_DOB_CHECK", inputs);
                if (rr.getDecision() == AiReasoner.Decision.APPROVE) {
                    return approve("AI-approved DOB change. " + rr.getReason());
                }
                if (rr.getDecision() == AiReasoner.Decision.REJECT) {
                    return reject(rr.getReason());
                }
                return reject("Documents do not appear to contain the requested date of birth.");
            }
            return approve(String.format(Locale.US,
                    "Auto-approved DOB change to %s based on provided documents.", newDob));
        }

        if (TYPE_ADDRESS.equalsIgnoreCase(type)) {
            String newAddr;
            Object addrObj = payload.get("address");
            if (addrObj instanceof Map<?, ?>) {
                newAddr = str(addrObj);
            } else {
                newAddr = str(addrObj);
            }
            if (isBlank(newAddr)) {
                newAddr = str(payload.get("fullAddress"));
            }
            if (isBlank(newAddr)) {
                return reject("Address change payload must include 'address'.");
            }
            if (eqIgnoreNull(profile.getAddress(), newAddr)) {
                return reject("Requested address matches current record.");
            }
            if (!containsIgnoreCase(docText, newAddr)) {
                // Consult AI before hard-rejecting
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("type", "ADDRESS_CHANGE");
                inputs.put("currentAddress", nz(profile.getAddress()));
                inputs.put("newAddress", nz(newAddr));
                inputs.put("docText", nz(safeTruncate(docText, 4000)));
                var rr = safeEval("SELF_SERVICE_ADDRESS_CHECK", inputs);
                if (rr.getDecision() == AiReasoner.Decision.APPROVE) {
                    return approve("AI-approved address change. " + rr.getReason());
                }
                if (rr.getDecision() == AiReasoner.Decision.REJECT) {
                    return reject(rr.getReason());
                }
                return reject("Documents do not appear to contain the requested address.");
            }
            return approve("Auto-approved address change based on provided documents.");
        }

        return reject("Unknown request type: " + type);
    }

    private String s(Object o) { return o == null ? null : String.valueOf(o); }

    // OCR helpers
    private String extractAllDocText(SelfServiceRequest r) {
        if (r.getDocuments() == null || r.getDocuments().isEmpty()) return "";
        Map<String, byte[]> docs = new HashMap<>();
        for (String rel : r.getDocuments()) {
            try {
                var resp = selfServiceClient.downloadDocument(r.getRequestId(), rel);
                if (resp != null && resp.getBody() != null) {
                    docs.put(rel, resp.getBody());
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
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) return false;
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private boolean docHasDate(String text, LocalDate date) {
        if (text == null || date == null) return false;
        String iso = date.toString(); // yyyy-MM-dd
        String dmyDash = String.format("%02d-%02d-%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        String dmySlash = String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        text = text.toLowerCase(Locale.ROOT);
        return text.contains(iso.toLowerCase(Locale.ROOT)) || text.contains(dmyDash.toLowerCase(Locale.ROOT)) || text.contains(dmySlash.toLowerCase(Locale.ROOT));
    }

    private AiReasoner.ReasoningResult safeEval(String task, Map<String, Object> inputs) {
        try {
            var rr = aiReasoner.evaluate(task, inputs);
            if (rr == null) {
                return new AiReasoner.ReasoningResult(AiReasoner.Decision.INCONCLUSIVE, "");
            }
            if (rr.getDecision() == null) {
                rr.setDecision(AiReasoner.Decision.INCONCLUSIVE);
            }
            return rr;
        } catch (Exception ex) {
            log.warn("SelfService: AI evaluate '{}' failed: {}", task, ex.getMessage());
            return new AiReasoner.ReasoningResult(AiReasoner.Decision.INCONCLUSIVE, "");
        }
    }
    private String str(Object o) { return o == null ? null : String.valueOf(o).trim(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean eqIgnoreNull(String a, String b) { return Objects.equals(nz(a), nz(b)); }
    private String nz(String s) { return s == null ? "" : s; }
    private String safeTruncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private Decision approve(String comment) { return new Decision("APPROVED", comment); }
    private Decision reject(String comment) { return new Decision("REJECTED", comment); }

    private record Decision(String decision, String comment) {}
}
