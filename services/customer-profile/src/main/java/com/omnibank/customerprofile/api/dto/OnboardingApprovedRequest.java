package com.omnibank.customerprofile.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardingApprovedRequest {
  @NotBlank
  @Size(max = 100)
  private String firstName;

  @NotBlank
  @Size(max = 100)
  private String lastName;

  @NotBlank
  @Email
  @Size(max = 150)
  private String email;

  @NotBlank
  @Size(max = 20)
  private String mobileNumber;

  // Optional: pass an external applicationId to trace linkage if needed
  @Size(max = 64)
  private String applicationId;
}
