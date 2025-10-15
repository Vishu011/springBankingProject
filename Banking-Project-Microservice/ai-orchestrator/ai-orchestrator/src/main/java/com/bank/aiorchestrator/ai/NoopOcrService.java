package com.bank.aiorchestrator.ai;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * No-op OCR implementation for Phase 1.
 * Returns empty text for all documents, but logs the request for observability.
 */
@Service
public class NoopOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(NoopOcrService.class);

    @Override
    public Map<String, String> extractText(List<String> documentPaths) {
        if (documentPaths == null || documentPaths.isEmpty()) {
            return Collections.emptyMap();
        }
        log.debug("NoopOcrService.extractText called for {} document(s).", documentPaths.size());
        return documentPaths.stream().collect(Collectors.toMap(p -> p, p -> ""));
    }

    @Override
    public Map<String, Double> qualityScores(List<String> documentPaths) {
        if (documentPaths == null || documentPaths.isEmpty()) {
            return Collections.emptyMap();
        }
        log.debug("NoopOcrService.qualityScores called for {} document(s).", documentPaths.size());
        return documentPaths.stream().collect(Collectors.toMap(p -> p, p -> 1.0d));
    }
}
