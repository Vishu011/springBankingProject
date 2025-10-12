package com.selfservice.proxy.dto;

import java.util.List;
import lombok.Data;

@Data
public class OtpGenerateRequest {
    private String userId;          // user UUID or email for public flows
    private String purpose;         // e.g., CONTACT_VERIFICATION, ACCOUNT_OPERATION
    private List<String> channels;  // e.g., ["EMAIL"]
    private String contextId;       // optional correlation, e.g., EMAIL_CHANGE:{userId}
    private Integer ttlSeconds;     // optional override
}
