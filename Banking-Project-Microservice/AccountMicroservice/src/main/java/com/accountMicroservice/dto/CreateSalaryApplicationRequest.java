package com.accountMicroservice.dto;

import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to submit a Salary/Corporate account application.
 * Flow:
 *  - Corporate email is verified via OTP service public endpoints (CONTACT_VERIFICATION).
 *  - Documents are paths/URLs previously uploaded by the client (we store references).
 */
@Data
public class CreateSalaryApplicationRequest {

    @NotBlank
    private String userId;

    @NotBlank
    @Email
    private String corporateEmail;

    @NotBlank
    private String otpCode;

    // Optional list of document references (URLs/paths); can be empty
    private List<String> documents;
}
