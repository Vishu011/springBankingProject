package com.otp.api.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateOtpRequest {
    @NotBlank
    private String userId;

    // LOGIN, WITHDRAWAL, LOAN_SUBMISSION, CARD_OPERATION, ACCOUNT_OPERATION, CONTACT_VERIFICATION
    @NotBlank
    private String purpose;

    // Currently only EMAIL is supported; keep list for future SMS support
    private List<String> channels;

    // Optional correlation id such as transactionId or registrationId
    private String contextId;

    // Optional custom TTL (seconds); if omitted, purpose-specific default is used
    @Min(30)
    private Integer ttlSeconds;
}
