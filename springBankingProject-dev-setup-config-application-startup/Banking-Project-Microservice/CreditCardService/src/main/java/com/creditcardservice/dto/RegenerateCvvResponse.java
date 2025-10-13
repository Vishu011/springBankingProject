package com.creditcardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for CVV regeneration. The clear CVV is returned once in this response.
 * Do NOT log or persist the plaintext CVV.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegenerateCvvResponse {
    private String cardId;
    private String cvv; // one-time reveal
    private String message;
}
