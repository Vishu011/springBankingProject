package com.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body sent to CreditCardService to validate a debit-card transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitCardValidationRequest {
    private String cardNumber;
    private String cvv;
}
