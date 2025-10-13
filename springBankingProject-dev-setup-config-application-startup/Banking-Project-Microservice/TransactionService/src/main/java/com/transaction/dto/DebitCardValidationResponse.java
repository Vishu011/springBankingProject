package com.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from CreditCardService for validating a debit card prior to withdrawal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitCardValidationResponse {
    private boolean valid;
    private String message;

    private String userId;
    private String accountId;
    private String brand;      // as plain string for cross-service DTO
    private String maskedPan;

    private Integer expiryMonth;
    private Integer expiryYear;
}
