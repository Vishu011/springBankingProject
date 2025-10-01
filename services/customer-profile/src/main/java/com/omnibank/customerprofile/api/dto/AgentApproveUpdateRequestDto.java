package com.omnibank.customerprofile.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentApproveUpdateRequestDto {
  @NotNull
  private Long requestId;

  @NotBlank
  @Size(max = 100)
  private String approvedBy;
}
