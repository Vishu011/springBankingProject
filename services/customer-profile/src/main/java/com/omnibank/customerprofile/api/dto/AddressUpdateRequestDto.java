package com.omnibank.customerprofile.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressUpdateRequestDto {
  @NotBlank
  @Size(max = 255)
  private String addressLine1;

  @NotBlank
  @Size(max = 100)
  private String city;
}
