package com.creditcardservice.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal debit request to TransactionService for fees like CARD_ISSUANCE_FEE.
 * Mirrors planned endpoint: POST /transactions/internal/debit
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
