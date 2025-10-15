package com.bank.aiorchestrator.integrations.user.dto;

/**
 * Mirror of UserMicroservice's KycReviewStatus.
 * Keep values in sync with the source service.
 */
public enum KycReviewStatus {
    SUBMITTED,
    APPROVED,
    REJECTED
}
