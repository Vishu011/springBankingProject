package com.omnibank.beneficiary.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyOtpRequest {

  @NotNull
  @Min(1)
  private Long owningCustomerId;

  @NotNull
  @Min(1)
  private Long beneficiaryId;

  @NotNull
  @Min(1)
  private Long challengeId;

  @NotBlank
  private String code;
}
