package com.omnibank.onboarding.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single document attached to an onboarding application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingDocument {
  private String type;     // e.g., PASSPORT, DRIVER_LICENSE, UTILITY_BILL
  private String url;      // storage location (for now just accept a URL/string ref)
  private DocumentStatus status; // UPLOADED, VERIFIED, REJECTED
}
