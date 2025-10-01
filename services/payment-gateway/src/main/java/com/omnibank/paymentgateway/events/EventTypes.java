package com.omnibank.paymentgateway.events;

public final class EventTypes {
  private EventTypes() {}

  public static final String PAYMENT_APPROVED_FOR_PROCESSING = "PaymentApprovedForProcessing";
  public static final String FRAUDULENT_TRANSACTION_BLOCKED = "FraudulentTransactionBlocked";
}
