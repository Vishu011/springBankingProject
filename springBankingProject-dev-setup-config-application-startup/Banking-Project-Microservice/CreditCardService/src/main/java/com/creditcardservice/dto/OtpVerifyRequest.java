package com.creditcardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to call otp-service /otp/verify from CreditCardService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {
    private String userId;
    // Expected: CARD_OPERATION
    private String purpose;
    private String contextId; // optional
    private String code;
}
