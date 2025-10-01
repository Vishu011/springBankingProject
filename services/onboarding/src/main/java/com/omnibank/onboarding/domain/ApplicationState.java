package com.omnibank.onboarding.domain;

public enum ApplicationState {
  STARTED,
  DOCUMENTS_UPLOADED,
  AWAITING_VERIFICATION,
  FLAGGED_FOR_MANUAL_REVIEW,
  VERIFIED,
  APPROVED,
  REJECTED
}
