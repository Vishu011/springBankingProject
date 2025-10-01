package com.omnibank.onboarding.api.dto;

import com.omnibank.onboarding.domain.ApplicationState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Secure/privileged request for an agent (human/admin or future AI) to update application status.
 */
@Data
public class AgentUpdateStatusRequest {

  @NotBlank
  @Size(max = 40)
  private String applicationId;

  @NotNull
  private ApplicationState newState; // VERIFIED, APPROVED, REJECTED, FLAGGED_FOR_MANUAL_REVIEW

  @NotBlank
  @Size(max = 100)
  private String agent; // e.g., "admin.user" or "AutomatedKYCAgentV2"

  @Size(max = 60)
  private String action; // e.g., "OCR_EXTRACTION", "AML_CHECK", "FINAL_DECISION"

  @Size(max = 20)
  private String outcome; // e.g., "SUCCESS", "PASS", "FAIL"

  private Double confidenceScore; // optional

  @Size(max = 4000)
  private String detailsJson; // optional structured details

  @Size(max = 4000)
  private String justification; // optional human-readable justification for audit
}
