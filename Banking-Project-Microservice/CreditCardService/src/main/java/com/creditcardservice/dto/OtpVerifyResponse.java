package com.creditcardservice.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors otp-service VerifyOtpResponse contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyResponse {
    private boolean verified;
    private String requestId;
    private LocalDateTime verifiedAt;
    private Integer remainingAttempts;
    private String message;
}
