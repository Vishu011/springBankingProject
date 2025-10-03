package com.omnibank.cardmanagement.events;

/**
 * Event type constants for card-management domain.
 */
public final class EventTypes {
  private EventTypes() {}

  public static final String CARD_CREATED = "CardCreated";
  public static final String CARD_STATUS_UPDATED = "CardStatusUpdated";
  public static final String CARD_LIMITS_CHANGED = "CardLimitsChanged";
}
