package com.bank.aiorchestrator.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Lightweight in-memory audit + idempotency service.
 * - Computes evidence hashes to avoid reprocessing the same item with same evidence.
 * - Stores minimal audit info for operator visibility (non-persistent). For production,
 *   back this with a DB table and retention policy.
 */
@Service
public class AuditService {

    private final ConcurrentHashMap<String, AuditEntry> store = new ConcurrentHashMap<>();

    public record AuditEntry(
            String workflow,
            String requestId,
            String userId,
            String action,       // APPROVED / REJECTED
            String reason,
            String evidenceHash,
            String mode,         // OFF / DRY_RUN / AUTO
            Instant decidedAt
    ) {}

    private static String key(String workflow, String requestId, String evidenceHash) {
        return (workflow == null ? "" : workflow) + "::" + (requestId == null ? "" : requestId) + "::" + (evidenceHash == null ? "" : evidenceHash);
    }

    /**
     * Returns true if the same workflow+request+evidence was already decided.
     */
    public boolean isDuplicate(String workflow, String requestId, String evidenceHash) {
        return store.containsKey(key(workflow, requestId, evidenceHash));
    }

    /**
     * Records an audit entry in memory.
     */
    public void record(String workflow, String requestId, String userId, String action, String reason, String evidenceHash, String mode) {
        AuditEntry entry = new AuditEntry(
                nz(workflow), nz(requestId), nz(userId),
                nz(action), nz(reason), nz(evidenceHash), nz(mode),
                Instant.now()
        );
        store.put(key(workflow, requestId, evidenceHash), entry);
    }

    /**
     * Compute a SHA-256 hash over a normalized set of evidence fields. Ensure stable ordering.
     * Example usage: pass a map containing fields relevant to the decision (e.g., balances, limits, doc fingerprints).
     */
    public String evidenceHash(Map<String, ?> evidence) {
        try {
            MessageDigest dig = MessageDigest.getInstance("SHA-256");
            // Create a deterministic representation by joining sorted keys with their stringified values.
            StringBuilder sb = new StringBuilder();
            if (evidence != null) {
                evidence.keySet().stream().sorted().forEach(k -> {
                    Object v = evidence.get(k);
                    sb.append(k).append('=').append(stringify(v)).append(';');
                });
            }
            byte[] bytes = dig.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            // Fallback: return simple toString hash if SHA-256 unavailable (unlikely)
            return Integer.toHexString(Objects.hashCode(evidence));
        }
    }

    private String stringify(Object v) {
        if (v == null) return "null";
        if (v instanceof Iterable<?> it) {
            StringBuilder b = new StringBuilder("[");
            boolean first = true;
            for (Object o : it) {
                if (!first) b.append(',');
                b.append(stringify(o));
                first = false;
            }
            return b.append(']').toString();
        }
        if (v instanceof Map<?, ?> m) {
            StringBuilder b = new StringBuilder("{");
            boolean first = true;
            for (Object k : m.keySet().stream().sorted((a, b2) -> String.valueOf(a).compareTo(String.valueOf(b2))).toList()) {
                if (!first) b.append(',');
                b.append(k).append(':').append(stringify(m.get(k)));
                first = false;
            }
            return b.append('}').toString();
        }
        return String.valueOf(v);
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
