package com.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal request to record a FINE transaction.
 * This does NOT move money; AccountMicroservice already applied the deduction or recovery.
 * We only persist a Transaction of type FINE with SUCCESS status and publish notification.
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
