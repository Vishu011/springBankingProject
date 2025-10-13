package com.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to perform a withdrawal using a debit card.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitCardWithdrawRequest {
    private String cardNumber;
    private String cvv;
    private Double amount;
    private String otpCode;
}
