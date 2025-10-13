package com.creditcardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to validate a debit card transaction before allowing withdrawal.
 * Contains plaintext CVV provided by the user. The service will hash and compare.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateDebitCardRequest {
    private String cardNumber;
    private String cvv;
}
