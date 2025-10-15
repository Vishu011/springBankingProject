package com.bank.aiorchestrator.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Central place to report and read queue sizes from workflows without creating bean cycles.
 * Workflows should inject this service and call setQueueSize(name, size).
 * OrchestratorService and controllers can read the snapshot.
 */
@Service
public class QueueMetricsService {

    private final ConcurrentHashMap<String, Integer> queues = new ConcurrentHashMap<>();

    public void setQueueSize(String name, int size) {
        if (name != null) {
            queues.put(name, Math.max(0, size));
        }
    }

    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(queues));
    }

    // Optional: initialize defaults so UI shows all keys even before first run
    public void ensureDefaults() {
        queues.putIfAbsent("kyc", 0);
        queues.putIfAbsent("loans", 0);
        queues.putIfAbsent("salary", 0);
        queues.putIfAbsent("cards", 0);
        queues.putIfAbsent("selfService", 0);
    }
}
