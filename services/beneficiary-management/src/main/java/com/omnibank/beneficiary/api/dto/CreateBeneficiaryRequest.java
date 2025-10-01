package com.omnibank.beneficiary.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBeneficiaryRequest {

  @NotBlank
  @Size(max = 100)
  private String nickname;

  @NotBlank
  @Size(max = 40)
  private String accountNumber;

  @NotBlank
  @Size(max = 20)
  private String bankCode; // e.g., IFSC/SWIFT or internal bank code
}
