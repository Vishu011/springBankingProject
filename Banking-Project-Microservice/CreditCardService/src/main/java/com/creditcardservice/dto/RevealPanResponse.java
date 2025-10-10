package com.creditcardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for revealing full PAN of a debit card after OTP verification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevealPanResponse {
    private String cardId;
    private String fullPan; // e.g., 5312 78xx xxxx 1234 (but here full, unmasked as requested)
    private String message;
}
