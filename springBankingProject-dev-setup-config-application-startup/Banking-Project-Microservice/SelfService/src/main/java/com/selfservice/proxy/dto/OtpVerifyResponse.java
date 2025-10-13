package com.selfservice.proxy.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyResponse {
    private boolean verified;
    private String requestId;          // OTP record id used
    private LocalDateTime verifiedAt;  // only set when verified = true
    private Integer remainingAttempts; // only set when verified = false
    private String message;
}
