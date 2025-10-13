package com.transaction.dto;

import java.util.List;
import lombok.Data;

@Data
public class OtpGenerateRequest {
    private String userId;          // email for public flows, or userId for authenticated flows
    private String purpose;         // CONTACT_VERIFICATION, ACCOUNT_OPERATION, etc.
    private List<String> channels;  // e.g., ["EMAIL"]
    private String contextId;       // correlation id e.g. STATEMENT:{accountId}:{from}_{to}
    private Integer ttlSeconds;     // optional
}
