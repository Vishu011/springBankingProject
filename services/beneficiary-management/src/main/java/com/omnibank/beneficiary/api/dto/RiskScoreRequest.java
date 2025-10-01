package com.omnibank.beneficiary.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class RiskScoreRequest {
  @NotNull
  @DecimalMin(value = "0.0", inclusive = true)
  @DecimalMax(value = "1.0", inclusive = true)
  private BigDecimal score;
}
