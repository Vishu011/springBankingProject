package com.bank.aiorchestrator.ai;

import java.util.List;
import java.util.Map;

/**
 * OCR abstraction for extracting text from uploaded documents (images/PDFs).
 * Phase 1: Provide a no-op implementation returning empty text to keep flows decoupled.
 * Phase 2: Implement using Apache Tika + PDFBox + tess4j.
 */
public interface OcrService {

    /**
     * Extract plain text per document path.
     * Document paths are relative to the corresponding microservice storage.
     * The caller service is responsible for downloading or fetching bytes if needed.
     * Default: empty map (no-op).
     */
    default Map<String, String> extractText(List<String> documentPaths) {
        return Map.of();
    }

    /**
     * Extract plain text per in-memory document bytes (keyed by a label/path).
     * Default: empty map (no-op).
     */
    default Map<String, String> extractTextFromBytes(Map<String, byte[]> documents) {
        return Map.of();
    }

    /**
     * Simple quality score [0.0, 1.0] per document if available.
     * Default no-op implementation returns empty map.
     */
    default Map<String, Double> qualityScores(List<String> documentPaths) {
        return Map.of();
    }
}
