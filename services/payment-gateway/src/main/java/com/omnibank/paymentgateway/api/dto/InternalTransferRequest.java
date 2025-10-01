package com.omnibank.paymentgateway.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Request body to initiate an internal transfer between two OmniBank accounts.
 */
@Data
public class InternalTransferRequest {

  @NotNull
  private Long customerId;

  @NotBlank
  private String fromAccount;

  @NotBlank
  private String toAccount;

  @NotNull
  @DecimalMin(value = "0.01", message = "amount must be greater than 0")
  private BigDecimal amount;

  @NotBlank
  private String currency; // e.g., "USD"
}
