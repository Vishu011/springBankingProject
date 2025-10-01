package com.omnibank.onboarding.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Minimal fields to start an onboarding application.
 * Extendable without breaking changes.
 */
@Data
public class StartOnboardingRequest {
  @NotBlank
  @Size(max = 100)
  private String firstName;

  @NotBlank
  @Size(max = 100)
  private String lastName;

  @NotBlank
  @Email
  private String email;

  @NotBlank
  @Size(max = 20)
  private String mobileNumber;
}
