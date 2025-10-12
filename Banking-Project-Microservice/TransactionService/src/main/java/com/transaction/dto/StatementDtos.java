package com.transaction.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs for account statement OTP flow and generation.
 */
public class StatementDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementInitiateRequest {
        @NotBlank
        private String userId;
        @NotBlank
        private String accountId;
        // If provided, OTP will be sent to this address; otherwise user's email will be used
        private String toEmail;
        @NotNull
        private LocalDate fromDate;
        @NotNull
        private LocalDate toDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementInitiateResponse {
        private String requestId;
        private LocalDateTime expiresAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementVerifyRequest {
        @NotBlank
        private String userId;
        @NotBlank
        private String accountId;
        private String toEmail;
        @NotNull
        private LocalDate fromDate;
        @NotNull
        private LocalDate toDate;
        @NotBlank
        private String code;
    }
}
