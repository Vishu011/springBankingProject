package com.bank.aiorchestrator.ai;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * OCR/Document text extraction using Apache Tika.
 * This implementation extracts text from provided in-memory bytes for PDFs/images/office docs.
 * Note: For more robust image OCR (scanned images), integrate tess4j later.
 */
@Primary
@Service
public class TikaOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(TikaOcrService.class);
    private final Tika tika = new Tika();

    @Override
    public Map<String, String> extractTextFromBytes(Map<String, byte[]> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> results = new HashMap<>();
        for (Map.Entry<String, byte[]> e : documents.entrySet()) {
            String key = e.getKey();
            byte[] bytes = e.getValue();
            if (bytes == null || bytes.length == 0) {
                results.put(key, "");
                continue;
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                String text = tika.parseToString(bais);
                // Normalize a bit to help matching downstream
                if (text != null) {
                    // Collapse excessive whitespace
                    text = text.replaceAll("\\s+", " ").trim();
                } else {
                    text = "";
                }
                results.put(key, text);
            } catch (TikaException te) {
                log.warn("Tika failed to parse '{}': {}", key, te.getMessage());
                results.put(key, "");
            } catch (Exception ex) {
                log.warn("Error extracting text for '{}': {}", key, ex.getMessage());
                results.put(key, "");
            }
        }
        return results;
    }
}
