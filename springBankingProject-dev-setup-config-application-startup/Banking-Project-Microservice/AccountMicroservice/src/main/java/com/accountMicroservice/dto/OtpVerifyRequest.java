package com.accountMicroservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to call otp-service /otp/verify from AccountMicroservice.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {
    private String userId;
    // Expected: ACCOUNT_OPERATION
    private String purpose;
    private String contextId; // optional
    private String code;
}
