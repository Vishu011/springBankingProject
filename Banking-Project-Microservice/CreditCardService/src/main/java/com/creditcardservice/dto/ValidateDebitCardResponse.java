package com.creditcardservice.dto;

import com.creditcardservice.model.CardBrand;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for validating a debit card prior to withdrawal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateDebitCardResponse {
    private boolean valid;
    private String message;

    private String userId;
    private String accountId;
    private CardBrand brand;
    private String maskedPan;

    private Integer expiryMonth;
    private Integer expiryYear;
}
