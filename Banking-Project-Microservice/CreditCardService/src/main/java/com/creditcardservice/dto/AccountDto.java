package com.creditcardservice.dto;

import java.time.LocalDateTime;

import com.creditcardservice.model.AccountStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    // Enum mirroring backend Account.java (made inner and public for cross-package access)
    public enum AccountType {
        SAVINGS,
        SALARY_CORPORATE
    }

    private String accountId;
    private String userId;
    private String accountNumber;
    private AccountType accountType;
    private Double balance;
    private AccountStatus status;
    private LocalDateTime createdAt;
}
