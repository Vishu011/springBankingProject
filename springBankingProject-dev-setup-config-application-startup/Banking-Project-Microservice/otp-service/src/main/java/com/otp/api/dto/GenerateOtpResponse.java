package com.otp.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateOtpResponse {
    private String requestId;     // The OTP record id
    private LocalDateTime expiresAt;
}
