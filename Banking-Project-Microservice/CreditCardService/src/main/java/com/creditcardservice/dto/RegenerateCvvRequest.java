package com.creditcardservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for regenerating a card's CVV.
 * Gate with OTP purpose CARD_OPERATION and owner-only checks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegenerateCvvRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String otpCode;
}
