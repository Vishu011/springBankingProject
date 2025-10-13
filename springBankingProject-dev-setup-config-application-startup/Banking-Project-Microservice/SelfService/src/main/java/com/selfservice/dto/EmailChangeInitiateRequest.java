package com.selfservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailChangeInitiateRequest {
    @NotBlank
    private String userId;     // current authenticated user's ID

    @NotBlank
    @Email
    private String newEmail;   // OTP will be sent to this new email (public OTP flow)
}
