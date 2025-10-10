package com.accountMicroservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.accountMicroservice.dao.AccountRepository;
import com.accountMicroservice.dto.AccountCreationRequest;
import com.accountMicroservice.dto.AccountResponse;
import com.accountMicroservice.dto.AccountUpdateRequest;
import com.accountMicroservice.dto.DepositRequest;
import com.accountMicroservice.dto.UserDto;
import com.accountMicroservice.dto.WithdrawRequest;
import com.accountMicroservice.dto.OtpVerifyRequest;
import com.accountMicroservice.dto.OtpVerifyResponse;
import com.accountMicroservice.proxyService.OtpServiceClient;
import com.accountMicroservice.exception.AccountCreationException;
import com.accountMicroservice.exception.AccountNotFoundException;
import com.accountMicroservice.exception.AccountProcessingException;
import com.accountMicroservice.exception.InsufficientFundsException;
import com.accountMicroservice.model.Account;
import com.accountMicroservice.model.AccountStatus;
import com.accountMicroservice.model.AccountType;
import com.accountMicroservice.proxyService.UserServiceClient;
import com.accountMicroservice.proxyService.NotificationServiceClient;
import com.accountMicroservice.proxyService.TransactionServiceClient;
import com.accountMicroservice.dto.NotificationRequest;
import com.accountMicroservice.dto.FineRequest;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserServiceClient userServiceClient;
    private final OtpServiceClient otpServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final TransactionServiceClient transactionServiceClient;

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository,
                              UserServiceClient userServiceClient,
                              OtpServiceClient otpServiceClient,
                              NotificationServiceClient notificationServiceClient,
                              TransactionServiceClient transactionServiceClient) {
        this.accountRepository = accountRepository;
        this.userServiceClient = userServiceClient;
        this.otpServiceClient = otpServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.transactionServiceClient = transactionServiceClient;
    }

    /**
     * Creates a new bank account for a user.
     */
    @Override
    @Transactional
    public AccountResponse createAccount(AccountCreationRequest request) {
        try {
            UserDto user = userServiceClient.getUserProfileById(request.getUserId());
            
            if (user == null) {
                throw new AccountCreationException("User not found with ID: " + request.getUserId());
            }

            if (user.getKycStatus() != UserDto.KycStatus.VERIFIED) {
                throw new AccountCreationException("Account creation denied: User KYC status is " + user.getKycStatus() + ". Must be VERIFIED.");
            }

            // OTP verification for account operation
            OtpVerifyRequest otpReq = new OtpVerifyRequest(request.getUserId(), "ACCOUNT_OPERATION", null, request.getOtpCode());
            OtpVerifyResponse otpRes = otpServiceClient.verify(otpReq);
            if (otpRes == null || !otpRes.isVerified()) {
                throw new AccountCreationException("Account creation denied: OTP verification failed" + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
            }

            // Enforce account count caps and creation rules
            long existing = accountRepository.countByUserIdAndAccountTypeAndStatusNot(
                request.getUserId(), request.getAccountType(), AccountStatus.CLOSED
            );
            if (request.getAccountType() == AccountType.SAVINGS) {
                if (existing >= 2) {
                    throw new AccountCreationException("Account creation denied: Maximum 2 SAVINGS accounts allowed (non-closed).");
                }
                if (request.getInitialBalance() < 2000) {
                    throw new AccountCreationException("Savings account requires an initial balance of at least 2000 INR.");
                }
                if (request.getInitialBalance() > 200000) {
                    throw new AccountCreationException("Savings account initial deposit exceeds the per-transaction limit of 200000 INR.");
                }
            } else if (request.getAccountType() == AccountType.SALARY_CORPORATE) {
                throw new AccountCreationException("Salary/Corporate accounts must be requested via application workflow and approved by an administrator.");
            }

            String newAccountNumber = generateUniqueAccountNumber();
            while (accountRepository.findByAccountNumber(newAccountNumber).isPresent()) {
                newAccountNumber = generateUniqueAccountNumber();
            }

            Account account = new Account();
            account.setUserId(request.getUserId());
            account.setAccountNumber(newAccountNumber);
            account.setAccountType(request.getAccountType());
            account.setBalance(request.getInitialBalance());
            account.setStatus(AccountStatus.ACTIVE);
            account.setCreatedAt(LocalDateTime.now());

            account = accountRepository.save(account);
            return mapToAccountResponse(account);

        } catch (DataIntegrityViolationException e) {
            throw new AccountCreationException("Failed to create account due to data integrity violation (e.g., duplicate account number).", e);
        } catch (HttpClientErrorException e) {
            throw new AccountProcessingException("Failed to validate user due to User Service error: " + e.getResponseBodyAsString(), e);
        } catch (AccountCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new AccountCreationException("Failed to create account: " + e.getMessage(), e);
        }
    }

    /**
     * Deposits funds into a specified account.
     */
    @Override
    @Transactional
    public AccountResponse depositFunds(String accountId, DepositRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (request.getAmount() <= 0) {
            throw new AccountProcessingException("Deposit amount must be positive.");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountProcessingException("Deposit denied: Account ID " + accountId + " is " + account.getStatus() + ".");
        }

        double amount = request.getAmount();
        double recovered = 0.0;
        Double pending = account.getPendingFineAmount() == null ? 0.0 : account.getPendingFineAmount();

        // Apply deposit to pending fine first (if any)
        if (pending > 0.0) {
            double recovery = Math.min(amount, pending);
            pending -= recovery;
            amount -= recovery;
            recovered = recovery;
            account.setPendingFineAmount(pending);
        }

        // Add any remaining amount to balance
        if (amount > 0.0) {
            account.setBalance(account.getBalance() + amount);
        }

        try {
            account = accountRepository.save(account);
            System.out.println("Deposit of " + request.getAmount() + " to account " + accountId + " for transaction " + request.getTransactionId() + " successful. Fine recovered: " + recovered);

            // Notify user about fine recovery if applicable
            if (recovered > 0.0) {
                String msg = "Pending fine recovery of INR " + recovered + " has been applied to your account. "
                        + (pending > 0.0 ? ("Remaining pending fine: INR " + pending + ".") : "Your pending fine is now fully recovered.");
                try {
                    notificationServiceClient.sendEmailNotification(new NotificationRequest(
                        account.getUserId(),
                        "EMAIL",
                        msg,
                        null
                    ));
                } catch (Exception ignore) {
                    // Best-effort notification; do not fail deposit
                    System.err.println("Failed to send fine recovery notification: " + ignore.getMessage());
                }
                // Record fine transaction for the recovered amount
                try {
                    transactionServiceClient.recordFine(new FineRequest(
                        account.getAccountId(),
                        recovered,
                        "Pending fine recovery of INR " + recovered + " recorded."
                    ));
                } catch (Exception ignore) {
                    System.err.println("Failed to record fine transaction for recovery: " + ignore.getMessage());
                }
            }

            return mapToAccountResponse(account);
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to process deposit for account ID: " + accountId, e);
        }
    }

    /**
     * Withdraws funds from a specified account.
     */
    @Override
    @Transactional
    public AccountResponse withdrawFunds(String accountId, WithdrawRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (request.getAmount() <= 0) {
            throw new AccountProcessingException("Withdrawal amount must be positive.");
        }
        if (account.getBalance() < request.getAmount()) {
            throw new InsufficientFundsException("Insufficient funds in account ID: " + accountId);
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountProcessingException("Withdrawal denied: Account ID " + accountId + " is " + account.getStatus() + ".");
        }

        double preBalance = account.getBalance();
        double amount = request.getAmount();
        double postBalance = preBalance - amount;

        // Set post-withdrawal balance
        account.setBalance(postBalance);

        // Fine detection for SAVINGS: transition from >=2000 to <2000
        if (account.getAccountType() == AccountType.SAVINGS && preBalance >= 2000.0 && postBalance < 2000.0) {
            double availableForFine = Math.min(200.0, Math.max(0.0, account.getBalance())); // Do not overdraw
            account.setBalance(account.getBalance() - availableForFine);

            double pendingAdd = 200.0 - availableForFine;
            if (pendingAdd > 0.0) {
                double currentPending = account.getPendingFineAmount() == null ? 0.0 : account.getPendingFineAmount();
                account.setPendingFineAmount(currentPending + pendingAdd);
            }

            // Record fine transaction for the portion actually deducted now
            if (availableForFine > 0.0) {
                try {
                    transactionServiceClient.recordFine(new FineRequest(
                        account.getAccountId(),
                        availableForFine,
                        "Minimum balance fine applied. Deducted now: INR " + availableForFine + ". Pending: INR " + pendingAdd + "."
                    ));
                } catch (Exception ignore) {
                    System.err.println("Failed to record fine transaction: " + ignore.getMessage());
                }
            }

            // Notify user
            String msg = pendingAdd > 0.0
                    ? ("Minimum balance not maintained. Fine of INR 200 applied: INR " + availableForFine + " deducted now; INR " + pendingAdd + " added as pending to be auto-recovered from next deposits.")
                    : "Minimum balance not maintained. Fine of INR 200 has been deducted from your account.";
            try {
                notificationServiceClient.sendEmailNotification(new NotificationRequest(
                    account.getUserId(),
                    "EMAIL",
                    msg,
                    null
                ));
            } catch (Exception ignore) {
                System.err.println("Failed to send min-balance fine notification: " + ignore.getMessage());
            }
        }

        try {
            account = accountRepository.save(account);
            System.out.println("Withdrawal of " + request.getAmount() + " from account " + accountId + " for transaction " + request.getTransactionId() + " successful.");
            return mapToAccountResponse(account);
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to process withdrawal for account ID: " + accountId, e);
        }
    }

    /**
     * Retrieves account details by account ID.
     */
    @Override
    public Optional<AccountResponse> getAccountById(String accountId) {
        return accountRepository.findById(accountId)
                                .map(this::mapToAccountResponse);
    }

    /**
     * Retrieves account details by account number.
     */
    @Override
    public Optional<AccountResponse> getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                                .map(this::mapToAccountResponse);
    }

    /**
     * Retrieves all accounts associated with a specific user ID.
     */
    @Override
    public List<AccountResponse> getAccountsByUserId(String userId) {
        return accountRepository.findByUserId(userId)
                                .stream()
                                .map(this::mapToAccountResponse)
                                .collect(Collectors.toList());
    }

    /**
     * Updates the status of an account.
     */
    @Override
    @Transactional
    public AccountResponse updateAccountStatus(String accountId, AccountUpdateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        // OTP verification for account status update
        OtpVerifyRequest otpReq = new OtpVerifyRequest(account.getUserId(), "ACCOUNT_OPERATION", null, request.getOtpCode());
        OtpVerifyResponse otpRes = otpServiceClient.verify(otpReq);
        if (otpRes == null || !otpRes.isVerified()) {
            throw new AccountProcessingException("Account update denied: OTP verification failed" + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
        }

        account.setStatus(request.getStatus());
        try {
            account = accountRepository.save(account);
            return mapToAccountResponse(account);
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to update account status for ID: " + accountId, e);
        }
    }

    /**
     * Deletes or closes an account.
     */
    @Override
    @Transactional
    public void deleteAccount(String accountId, String otpCode) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        // OTP verification for account deletion
        OtpVerifyRequest otpReq = new OtpVerifyRequest(account.getUserId(), "ACCOUNT_OPERATION", null, otpCode);
        OtpVerifyResponse otpRes = otpServiceClient.verify(otpReq);
        if (otpRes == null || !otpRes.isVerified()) {
            throw new AccountProcessingException("Account deletion denied: OTP verification failed" + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
        }

        try {
            accountRepository.delete(account);
            System.out.println("Account with ID: " + accountId + " deleted successfully.");
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to delete account with ID: " + accountId, e);
        }
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    /**
     * Helper method to generate a unique 10-digit account number.
     * Ensures the number is always positive.
     */
    private String generateUniqueAccountNumber() {
        // Get the absolute value of the most significant bits to ensure a positive number
        // Modulo by 10 billion to get a number within 10 digits
        long positiveNumber = Math.abs(UUID.randomUUID().getMostSignificantBits() % 10_000_000_000L);
        // Format to a 10-digit string with leading zeros if necessary
        return String.format("%010d", positiveNumber);
    }
}
