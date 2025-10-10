package com.bank.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to call otp-service /otp/verify from loan-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {
    private String userId;
    // Expected: LOAN_SUBMISSION, WITHDRAWAL, etc.
    private String purpose;
    private String contextId; // optional (e.g., loanId/correlationId)
    private String code;
}
