package com.selfservice.proxy.dto;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String userId;    // email for public flows, userId for authenticated flows
    private String purpose;   // CONTACT_VERIFICATION, ACCOUNT_OPERATION, etc.
    private String contextId; // optional correlation (e.g., EMAIL_CHANGE:{userId} or PHONE_CHANGE:{userId})
    private String code;      // received OTP
}
