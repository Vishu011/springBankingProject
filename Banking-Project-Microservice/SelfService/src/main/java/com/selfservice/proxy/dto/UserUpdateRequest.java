package com.selfservice.proxy.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror of UserMicroservice's UserUpdateRequest for Feign updates.
 * Keep fields optional; only non-null values will be applied by User service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String username;
    private String email;
    private String role;         // string role if ever needed
    private String firstName;
    private String middleName;   // added to support middle name updates
    private String lastName;
    private LocalDate dateOfBirth;
    private String address;      // formatted full address (line1, line2, city, state, postalCode, country)
    private String phoneNumber;
    private String kycStatus;    // string status if ever needed
}
