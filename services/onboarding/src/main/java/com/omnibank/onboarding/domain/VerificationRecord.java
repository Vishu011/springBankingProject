package com.omnibank.onboarding.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Captures each verification step performed by an automated agent or human.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRecord {
  private String agent;           // e.g., AutomatedKYCAgentV2 or admin username
  private Instant timestamp;
  private String action;          // e.g., OCR_EXTRACTION, AML_CHECK
  private String outcome;         // e.g., SUCCESS, PASS, FAIL
  private Double confidenceScore; // optional
  private String detailsJson;     // optional JSON string with details (for simplicity)
}
