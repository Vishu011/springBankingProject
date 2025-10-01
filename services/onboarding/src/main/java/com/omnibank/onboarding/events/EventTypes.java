package com.omnibank.onboarding.events;

public final class EventTypes {
  private EventTypes() {}

  public static final String APPLICATION_STARTED = "ApplicationStarted";
  public static final String DOCUMENTS_UPLOADED = "DocumentsUploaded";
  public static final String VERIFICATION_REQUIRED = "VerificationRequired";
  public static final String ONBOARDING_APPROVED = "OnboardingApproved";
  public static final String MANUAL_REVIEW_REQUIRED = "ManualReviewRequired";
}
