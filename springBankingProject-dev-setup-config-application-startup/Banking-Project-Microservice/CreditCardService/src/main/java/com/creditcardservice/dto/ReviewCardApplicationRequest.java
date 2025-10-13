package com.creditcardservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCardApplicationRequest {

    public enum Decision {
        APPROVED, REJECTED
    }

    @NotNull
    private Decision decision;

    @NotBlank
    private String reviewerId;

    // Required if decision=APPROVED and type=CREDIT
    private Double approvedLimit;

    // Optional override; if null -> default now + 5 years
    private Integer expiryMonth; // 1-12
    private Integer expiryYear;  // YYYY

    private String adminComment;
}
