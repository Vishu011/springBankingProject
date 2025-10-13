package com.accountMicroservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Local DTO mirroring TransactionService's FineRequest.
 * Used by Feign client to record FINE transactions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FineRequest {

    @NotBlank(message = "Account ID cannot be empty")
    private String accountId;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private Double amount;

    // Optional human-readable message to include in notification/event
    private String message;
}
