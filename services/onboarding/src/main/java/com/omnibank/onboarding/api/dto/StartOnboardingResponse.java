package com.omnibank.onboarding.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartOnboardingResponse {
  private String applicationId;
  private String status;
}
