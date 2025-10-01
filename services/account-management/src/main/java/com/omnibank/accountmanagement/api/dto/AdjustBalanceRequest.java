package com.omnibank.accountmanagement.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdjustBalanceRequest {
  @NotNull
  private BigDecimal amount; // positive = credit, negative = debit
}
