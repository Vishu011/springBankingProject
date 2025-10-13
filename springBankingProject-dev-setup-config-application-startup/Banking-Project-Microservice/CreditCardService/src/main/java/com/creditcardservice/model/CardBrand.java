package com.creditcardservice.model;

/**
 * Card network brands for PAN/CVV rules.
 * Note: CVV length may vary by account type per business rules (3 for Savings: VISA/RUPAY, 4 for Salary/Corporate: AMEX/MASTERCARD/DISCOVERY).
 * Do not assume cvvLength directly from brand; enforce in service logic using account type.
 */
public enum CardBrand {
    VISA,
    RUPAY,
    MASTERCARD,
    AMEX,
    DISCOVERY
}
