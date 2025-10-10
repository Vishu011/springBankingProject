package com.creditcardservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to reveal full PAN for a user's own debit card.
 * Requires OTP verification (CARD_OPERATION) before returning PAN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevealPanRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String otpCode;
}
