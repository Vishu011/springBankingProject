package com.creditcardservice.dto;

import com.creditcardservice.model.CardBrand;
import com.creditcardservice.model.CardKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCardApplicationRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String accountId;

    @NotNull
    private CardKind type; // CREDIT or DEBIT

    @NotNull
    private CardBrand requestedBrand;

    @NotBlank
    private String otpCode;
}
