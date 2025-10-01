package com.omnibank.paymentgateway.domain;

/**
 * Orchestration status for a payment request.
 */
public enum PaymentStatus {
  RECEIVED,
  PROCESSING,
  MFA_CHALLENGE_REQUIRED,
  BLOCKED,
  COMPLETED,
  REJECTED
}
