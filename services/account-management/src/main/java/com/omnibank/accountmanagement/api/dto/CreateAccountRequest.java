package com.omnibank.accountmanagement.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAccountRequest {
  @NotNull
  @Min(1)
  private Long customerId;

  @NotBlank
  @Size(max = 30)
  private String accountType; // SAVINGS, CURRENT, FD, RD
}
