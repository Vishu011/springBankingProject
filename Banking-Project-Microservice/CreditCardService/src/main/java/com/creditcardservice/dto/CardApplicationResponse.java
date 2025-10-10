package com.creditcardservice.dto;

import com.creditcardservice.model.CardBrand;
import com.creditcardservice.model.CardKind;
import com.creditcardservice.model.CardApplication.ApplicationStatus;
import lombok.Data;

@Data
public class CardApplicationResponse {
    // Show-once CVV for admin approve response only (never persisted).
    // This will be populated only when approval is successful, and never returned again.
    private String oneTimeCvv;
    private String applicationId;
    private String userId;
    private String accountId;
    private CardKind type;
    private CardBrand requestedBrand;
    private ApplicationStatus status;

    private Integer issueMonth;
    private Integer issueYear;
    private Integer expiryMonth;
    private Integer expiryYear;

    private Double approvedLimit; // for CREDIT only

    // Sensitive data masked for display only
    private String maskedPan;      // e.g., **** **** **** 1234
    private String maskedCvv;      // e.g., *** or ****

    private String reviewerId;
    private String adminComment;

    private String submittedAt;
    private String reviewedAt;
}
