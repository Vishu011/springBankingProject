package com.bank.aiorchestrator.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.bank.aiorchestrator.ai.AiReasoner;
import com.bank.aiorchestrator.ai.OcrService;
import com.bank.aiorchestrator.integrations.account.SalaryAdminClient;
import com.bank.aiorchestrator.integrations.account.dto.SalaryApplicationResponse;
import com.bank.aiorchestrator.model.AgentMode;
import com.bank.aiorchestrator.service.AgentStateService;
import com.bank.aiorchestrator.service.QueueMetricsService;

/**
 * Automates Salary/Corporate account applications.
 * Initial deterministic rules (LLM/OCR can enhance later):
 *  - Corporate email must contain a valid domain like '@company.com'
 *  - At least one supporting document must be uploaded
 *  Approves if both satisfied, otherwise rejects with a clear reason.
 */
@Service
public class SalaryWorkflow {

    private static final Logger log = LoggerFactory.getLogger(SalaryWorkflow.class);

    private final SalaryAdminClient salaryClient;
    private final AgentStateService agentStateService;
    private final QueueMetricsService queueMetricsService;
    private final OcrService ocrService;
    private final AiReasoner aiReasoner;

    public SalaryWorkflow(SalaryAdminClient salaryClient,
                          AgentStateService agentStateService,
                          QueueMetricsService queueMetricsService,
                          @Qualifier("tikaOcrService") OcrService ocrService,
                          AiReasoner aiReasoner) {
        this.salaryClient = salaryClient;
        this.agentStateService = agentStateService;
        this.queueMetricsService = queueMetricsService;
        this.ocrService = ocrService;
        this.aiReasoner = aiReasoner;
    }

    public void process() {
        List<SalaryApplicationResponse> apps;
        try {
            apps = salaryClient.listByStatus("SUBMITTED");
        } catch (Exception ex) {
            log.error("Salary: failed to fetch SUBMITTED applications: {}", ex.getMessage());
            return;
        }
        if (apps == null || apps.isEmpty()) {
            log.debug("Salary: no SUBMITTED applications");
            queueMetricsService.setQueueSize("salary", 0);
            return;
        }

        int pending = 0;
        for (SalaryApplicationResponse app : apps) {
            if (!"SUBMITTED".equalsIgnoreCase(s(app.getStatus()))) continue;
            pending++;

            Decision d = decide(app);
            if (agentStateService.getMode() == AgentMode.DRY_RUN) {
                log.info("Salary[DRY_RUN]: appId={} user={} decision={} comment='{}'",
                        app.getApplicationId(), app.getUserId(), d.decision, d.comment);
                continue;
            }

            if ("SKIP".equalsIgnoreCase(d.decision)) {
                log.warn("Salary: skipping app {} (transient doc processing issue); will retry later", app.getApplicationId());
                continue;
            }
            if (agentStateService.getMode() == AgentMode.OFF) {
                log.debug("Salary: Agent mode OFF; skipping reviews");
                break;
            }

            // Re-validate current status to avoid backend 500 if already reviewed elsewhere
            var current = salaryClient.getOne(app.getApplicationId());
            if (current == null || !"SUBMITTED".equalsIgnoreCase(s(current.getStatus()))) {
                log.warn("Salary: app {} no longer SUBMITTED (status={}); skipping review",
                        app.getApplicationId(), s(current == null ? null : current.getStatus()));
                continue;
            }

            try {
                var resp = salaryClient.review(app.getApplicationId(),
                        d.decision, d.comment, "agent");
                log.info("Salary: reviewed app {} for user {} -> status={}",
                        app.getApplicationId(), app.getUserId(), s(resp.getStatus()));
            } catch (Exception ex) {
                log.error("Salary: review failed for app {}: {}", app.getApplicationId(), ex.getMessage());
            }
        }
        queueMetricsService.setQueueSize("salary", pending);
    }

    private Decision decide(SalaryApplicationResponse app) {
        String email = s(app.getCorporateEmail());
        boolean hasDocs = app.getDocuments() != null && !app.getDocuments().isEmpty();

        if (email == null || !email.contains("@")) {
            return new Decision("REJECTED", "Corporate email is missing or invalid.");
        }
        if (!hasDocs) {
            return new Decision("REJECTED", "Auto-rejected: no supporting documents uploaded.");
        }

        // Email is OTP-verified on user side; here we only derive domain/company token and corroborate via documents/AI.
        DocAgg agg = extractAllDocText(app); // OCR text + transient error flag
        String docText = agg.text(); // already lowercased
        if (agg.hadErrors() && (docText == null || docText.isBlank())) {
            return new Decision("SKIP", "Document retrieval/OCR transient error; will retry later.");
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        String token = domainToToken(domain); // e.g., oracle.com -> oracle

        // Deterministic corroboration: accept if docs mention the domain or the company token as a whole word.
        if (containsCompanyEvidence(docText, domain, token)) {
            return new Decision("APPROVED", String.format(Locale.US,
                    "Auto-approved: domain '%s' and company reference corroborated in submitted documents.", domain));
        }

        // Consult AI to assess if the domain corresponds to a valid company and whether docs imply employment.
        Map<String, Object> inputs = Map.of(
                "corporateEmail", email,
                "domain", domain,
                "companyToken", token,
                "docText", safeTruncate(docText, 4000)
        );
        var rr = aiReasoner.evaluate("SALARY_EMPLOYMENT_CHECK", inputs);
        if (rr.getDecision() == com.bank.aiorchestrator.ai.AiReasoner.Decision.APPROVE) {
            return new Decision("APPROVED", "AI-approved salary/corporate application. " + rr.getReason());
        }
        if (rr.getDecision() == com.bank.aiorchestrator.ai.AiReasoner.Decision.REJECT) {
            return new Decision("REJECTED", rr.getReason());
        }
        return new Decision("REJECTED", String.format(Locale.US,
                "Corporate domain '%s' appears valid but documents do not reference the company. Please upload ID/offer letter showing company name or domain.", domain));
    }

    private String s(Object o) { return o == null ? null : String.valueOf(o); }

    // OCR helpers for salary documents
    private DocAgg extractAllDocText(SalaryApplicationResponse app) {
        boolean hadErrors = false;
        if (app.getDocuments() == null || app.getDocuments().isEmpty()) return new DocAgg("", false);
        Map<String, byte[]> docs = new HashMap<>();
        for (String rel : app.getDocuments()) {
            try {
                var resp = salaryClient.downloadDocument(app.getApplicationId(), rel);
                if (resp != null && resp.body() != null) {
                    docs.put(rel, resp.body().asInputStream().readAllBytes());
                } else {
                    hadErrors = true;
                }
            } catch (Exception e) {
                hadErrors = true;
            }
        }
        Map<String, String> texts = ocrService.extractTextFromBytes(docs);
        StringBuilder sb = new StringBuilder();
        if (texts != null) {
            for (String t : texts.values()) {
                if (t != null) sb.append(' ').append(t);
            }
        } else {
            hadErrors = true;
        }
        return new DocAgg(sb.toString().toLowerCase(Locale.ROOT), hadErrors);
    }

    private String safeTruncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    // True if documents contain explicit company evidence via domain or a whole-word company token
    private boolean containsCompanyEvidence(String docText, String domain, String token) {
        if (docText == null) return false;
        String lc = docText.toLowerCase(Locale.ROOT);

        // direct domain mention (e.g., oracle.com)
        if (domain != null && !domain.isBlank() && lc.contains(domain.toLowerCase(Locale.ROOT))) {
            return true;
        }
        // tolerate OCR dropping punctuation (e.g., "oracle com" or "oraclecom")
        if (domain != null && !domain.isBlank()) {
            String normDoc = lc.replaceAll("[^a-z0-9]", "");
            String normDomain = domain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (!normDomain.isBlank() && normDoc.contains(normDomain)) {
                return true;
            }
        }

        if (token != null && !token.isBlank()) {
            String t = token.toLowerCase(Locale.ROOT);
            // whole-word token (e.g., "oracle")
            if (containsWord(lc, t)) return true;

            // fuzzy similarity against document tokens (to handle minor OCR/name variations)
            double sim = bestSimilarity(t, lc);
            if (sim >= 0.75) return true;
        }
        return false;
    }

    // Whole word check: matches 'oracle' but not 'coracle'
    private boolean containsWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) return false;
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find();
    }

    // Compute best similarity (0..1) of token against any word in the text using normalized Levenshtein
    private double bestSimilarity(String token, String text) {
        if (token == null || token.isBlank() || text == null || text.isBlank()) return 0.0;
        String[] words = text.split("[^a-z0-9]+");
        double best = 0.0;
        for (String w : words) {
            if (w == null || w.length() < 3) continue;
            int maxLen = Math.max(token.length(), w.length());
            if (maxLen == 0) continue;
            int dist = levenshtein(token, w);
            double sim = 1.0 - ((double) dist / (double) maxLen);
            if (sim > best) best = sim;
            if (best >= 0.95) break; // early exit on very high confidence
        }
        return best;
    }

    // Levenshtein distance
    private int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = (ca == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }

    // Convert a domain like 'oracle.com' or 'mail.google.com' into a token 'oracle' or 'google'
    private String domainToToken(String domain) {
        if (domain == null) return null;
        String d = domain.toLowerCase(Locale.ROOT);
        // normalize some common prefixes but prefer using registrable (second-level) domain
        if (d.startsWith("www.")) d = d.substring(4);
        if (d.startsWith("mail.")) d = d.substring(5);
        if (d.startsWith("id.")) d = d.substring(3);
        String[] parts = d.split("\\.");
        if (parts.length == 0) return null;

        int n = parts.length;
        String last = parts[n - 1];
        String secondLast = n >= 2 ? parts[n - 2] : null;

        // Handle ccTLD patterns like company.co.in, company.com.au, company.co.uk -> pick 'company'
        boolean ccTld = "in".equals(secondSafe(last)) || "uk".equals(secondSafe(last)) || "au".equals(secondSafe(last)) || "za".equals(secondSafe(last));
        boolean registrarLike = "co".equals(secondSafe(secondLast)) || "com".equals(secondSafe(secondLast)) || "net".equals(secondSafe(secondLast)) || "org".equals(secondSafe(secondLast));
        String candidate;
        if (n >= 3 && ccTld && registrarLike) {
            candidate = parts[n - 3];
        } else if (n >= 2) {
            candidate = parts[n - 2];
        } else {
            candidate = parts[0];
        }
        String token = candidate.replaceAll("[^a-z0-9]", "");
        return token;
    }

    private String secondSafe(String s) {
        return s == null ? "" : s;
    }

    private record DocAgg(String text, boolean hadErrors) {}
    private record Decision(String decision, String comment) {}
}
