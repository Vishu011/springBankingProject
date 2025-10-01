package com.omnibank.accountmanagement.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * Dev-only DTO mirroring ledger's TransactionPosted event payload for webhook consumption.
 * Matches com.omnibank.ledger.application.LedgerService.EventPayload JSON shape.
 */
@Data
public class LedgerTransactionPostedDto {

  @NotBlank
  private String transactionId;

  @NotEmpty
  @Valid
  private List<PostedEntry> entries;

  @Data
  public static class PostedEntry {
    @NotBlank
    private String account; // accountNumber

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    private BigDecimal amount;

    @NotNull
    private Character direction; // 'D' or 'C'
  }
}
