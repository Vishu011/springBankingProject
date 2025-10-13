package com.otp.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String userId;

    // LOGIN, WITHDRAWAL, LOAN_SUBMISSION, CARD_OPERATION, ACCOUNT_OPERATION, CONTACT_VERIFICATION
    @NotBlank
    private String purpose;

    // Optional correlation id such as transactionId or registrationId
    private String contextId;

    @NotBlank
    private String code;
}
