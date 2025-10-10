package com.transaction.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal debit request to support fees like CARD_ISSUANCE_FEE.
 * Consumed via POST /transactions/internal/debit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalDebitRequest {
    private String accountId;
    private Double amount;
    private String reason; // e.g., CARD_ISSUANCE_FEE
    private Map<String, String> metadata; // e.g., { "type": "CREDIT", "brand": "VISA" }
}
