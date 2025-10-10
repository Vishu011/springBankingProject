package com.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to call otp-service /otp/verify
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {
    private String userId;
    // Expected: WITHDRAWAL, LOAN_SUBMISSION, etc.
    private String purpose;
    private String contextId; // optional (e.g., transactionId)
    private String code;
}
