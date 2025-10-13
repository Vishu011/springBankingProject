package com.transaction.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.transaction.dao.TransactionRepository;
import com.transaction.dto.AccountDto;
import com.transaction.dto.DepositRequest;
import com.transaction.dto.DepositRequestDto;
import com.transaction.dto.NotificationRequestDto;
import com.transaction.dto.TransferRequest; // Updated DTO
import com.transaction.dto.UserDto;
import com.transaction.dto.WithdrawRequest;
import com.transaction.dto.WithdrawRequestDto;
import com.transaction.dto.FineRequest;
import com.transaction.event.TransactionCompletedEvent;
import com.transaction.exceptions.AccountNotFoundException;
import com.transaction.exceptions.InsufficientFundsException;
import com.transaction.exceptions.InvalidTransactionException;
import com.transaction.exceptions.TransactionProcessingException;
import com.transaction.exceptions.UnauthorizedUserException;
import com.transaction.model.Transaction;
import com.transaction.model.TransactionStatus;
import com.transaction.model.TransactionType;
import com.transaction.proxyService.AccountServiceClient;
import com.transaction.proxyService.LoanServiceClient;
import com.transaction.proxyService.NotificationServiceClient;
import com.transaction.proxyService.UserServiceClient;
import com.transaction.proxyService.OtpServiceClient;
import com.transaction.dto.OtpVerifyRequest;
import com.transaction.dto.OtpVerifyResponse;
import com.transaction.dto.InternalDebitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.dto.DebitCardWithdrawRequest;
import com.transaction.dto.DebitCardValidationRequest;
import com.transaction.dto.DebitCardValidationResponse;
import com.transaction.proxyService.CreditCardServiceClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final LoanServiceClient loanServiceClient;
    private final KafkaTemplate<String, TransactionCompletedEvent> kafkaTemplate;
    private final NotificationServiceClient notificationServiceClient;
    private final UserServiceClient userServiceClient;
    
    @Autowired
    private OtpServiceClient otpServiceClient;

    @Autowired
    private CreditCardServiceClient creditCardServiceClient;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                              AccountServiceClient accountServiceClient,
                              LoanServiceClient loanServiceClient,
                              KafkaTemplate<String, TransactionCompletedEvent> kafkaTemplate,
                              NotificationServiceClient notificationServiceClient,
                              UserServiceClient userServiceClient) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.loanServiceClient = loanServiceClient;
        this.kafkaTemplate = kafkaTemplate;
        this.notificationServiceClient = notificationServiceClient;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Helper method for KYC check.
     */
    private void checkKycStatus(String userId) {
        UserDto userProfile = userServiceClient.getUserProfileById(userId);
        if (userProfile == null) {
            throw new UnauthorizedUserException("User profile not found for transaction. Cannot proceed.");
        }
        if (userProfile.getKycStatus() != UserDto.KycStatus.VERIFIED) {
            throw new UnauthorizedUserException("Transaction denied: User KYC status is " + userProfile.getKycStatus() + ". Must be VERIFIED.");
        }
    }

    /**
     * Processes a deposit transaction.
     */
    @Override
    @Transactional
    public Transaction deposit(DepositRequest request) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(null);
        transaction.setToAccountId(request.getAccountId());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        try {
            AccountDto targetAccount = accountServiceClient.getAccountById(request.getAccountId());
            if (targetAccount == null) {
                throw new AccountNotFoundException("Target account not found with ID: " + request.getAccountId());
            }
            // Add metadata: account-based operation (deposit into target)
            try {
                java.util.Map<String, String> meta = new java.util.HashMap<>();
                meta.put("method", "ACCOUNT");
                if (targetAccount.getAccountNumber() != null) {
                    meta.put("toAccountNumber", targetAccount.getAccountNumber());
                    meta.put("toAccountMasked", maskAccount(targetAccount.getAccountNumber()));
                }
                transaction.setMetadataJson(new ObjectMapper().writeValueAsString(meta));
            } catch (Exception ignore) {}

            checkKycStatus(targetAccount.getUserId());

            // Enforce per-transaction limits and balance cap by account type
            if (targetAccount.getAccountType() == AccountDto.AccountType.SAVINGS) {
                if (request.getAmount() > 200000) {
                    throw new InvalidTransactionException("Deposit exceeds maximum per-transaction limit of 200000 INR for SAVINGS account.");
                }
            } else if (targetAccount.getAccountType() == AccountDto.AccountType.SALARY_CORPORATE) {
                if (request.getAmount() > 500000) {
                    throw new InvalidTransactionException("Deposit exceeds maximum per-transaction limit of 500000 INR for SALARY/CORPORATE account.");
                }
                if (targetAccount.getBalance() + request.getAmount() > 10000000) {
                    throw new InvalidTransactionException("Deposit would exceed the maximum balance cap of 10000000 INR for SALARY/CORPORATE account.");
                }
            }

            DepositRequestDto depositRequestDto = new DepositRequestDto(transaction.getTransactionId(), request.getAmount());
            accountServiceClient.depositFunds(request.getAccountId(), depositRequestDto);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction = transactionRepository.save(transaction);

            String notificationMessage = "A deposit of " + request.getAmount() + " has been made to your account " + targetAccount.getAccountNumber() + ". Transaction ID: " + transaction.getTransactionId();
            publishTransactionCompletedEvent(
                transaction.getTransactionId(),
                targetAccount.getUserId(),
                targetAccount.getAccountId(),
                request.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                notificationMessage
            );

        } catch (HttpClientErrorException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Deposit failed due to Account Service error: " + e.getResponseBodyAsString(), e);
        } catch (AccountNotFoundException | UnauthorizedUserException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Deposit failed unexpectedly: " + e.getMessage(), e);
        }
        return transaction;
    }

    /**
     * Processes a withdrawal transaction.
     */
    @Override
    @Transactional
    public Transaction withdraw(WithdrawRequest request) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(request.getAccountId());
        transaction.setToAccountId(null);
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        try {
            AccountDto sourceAccount = accountServiceClient.getAccountById(request.getAccountId());
            if (sourceAccount == null) {
                throw new AccountNotFoundException("Source account not found with ID: " + request.getAccountId());
            }
            // Add metadata: account-based operation (withdraw from source)
            try {
                java.util.Map<String, String> meta = new java.util.HashMap<>();
                meta.put("method", "ACCOUNT");
                if (sourceAccount.getAccountNumber() != null) {
                    meta.put("fromAccountNumber", sourceAccount.getAccountNumber());
                    meta.put("fromAccountMasked", maskAccount(sourceAccount.getAccountNumber()));
                }
                transaction.setMetadataJson(new ObjectMapper().writeValueAsString(meta));
            } catch (Exception ignore) {}

            checkKycStatus(sourceAccount.getUserId());

            // Enforce per-transaction limits by account type
            if (sourceAccount.getAccountType() == AccountDto.AccountType.SAVINGS) {
                if (request.getAmount() > 200000) {
                    throw new InvalidTransactionException("Withdrawal exceeds maximum per-transaction limit of 200000 INR for SAVINGS account.");
                }
            } else if (sourceAccount.getAccountType() == AccountDto.AccountType.SALARY_CORPORATE) {
                if (request.getAmount() > 500000) {
                    throw new InvalidTransactionException("Withdrawal exceeds maximum per-transaction limit of 500000 INR for SALARY/CORPORATE account.");
                }
            }

            if (sourceAccount.getBalance() < request.getAmount()) {
                throw new InsufficientFundsException("Insufficient funds in account: " + request.getAccountId());
            }

            // OTP verification before proceeding
            OtpVerifyRequest otpReq = new OtpVerifyRequest(
                sourceAccount.getUserId(),
                "WITHDRAWAL",
                null,
                request.getOtpCode()
            );
            OtpVerifyResponse otpRes = otpServiceClient.verify(otpReq);
            if (otpRes == null || !otpRes.isVerified()) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                throw new TransactionProcessingException("OTP verification failed: " + (otpRes != null ? otpRes.getMessage() : "no response"));
            }

            WithdrawRequestDto withdrawRequestDto = new WithdrawRequestDto(transaction.getTransactionId(), request.getAmount());
            accountServiceClient.withdrawFunds(request.getAccountId(), withdrawRequestDto);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction = transactionRepository.save(transaction);

            String notificationMessage = "A withdrawal of " + request.getAmount() + " has been made from your account " + sourceAccount.getAccountNumber() + ". Transaction ID: " + transaction.getTransactionId();
            publishTransactionCompletedEvent(
                transaction.getTransactionId(),
                sourceAccount.getUserId(),
                sourceAccount.getAccountId(),
                request.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                notificationMessage
            );

        } catch (HttpClientErrorException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Withdrawal failed due to Account Service error: " + e.getResponseBodyAsString(), e);
        } catch (AccountNotFoundException | InsufficientFundsException | InvalidTransactionException | UnauthorizedUserException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Withdrawal failed unexpectedly: " + e.getMessage(), e);
        }
        return transaction;
    }

    /**
     * Processes a fund transfer transaction between two accounts.
     * Updated to use account numbers.
     */
    @Override
    @Transactional
    public Transaction transfer(TransferRequest request) {
        Transaction transaction = new Transaction();
        // Set initial account IDs to null, they will be resolved
        transaction.setFromAccountId(null);
        transaction.setToAccountId(null);
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        try {
            // Resolve account numbers to account IDs
            AccountDto sourceAccount = accountServiceClient.getAccountByAccountNumber(request.getFromAccountNumber());
            AccountDto targetAccount = accountServiceClient.getAccountByAccountNumber(request.getToAccountNumber());

            if (sourceAccount == null) {
                throw new AccountNotFoundException("Source account not found with number: " + request.getFromAccountNumber());
            }
            if (targetAccount == null) {
                throw new AccountNotFoundException("Target account not found with number: " + request.getToAccountNumber());
            }

            // Set resolved account IDs to the transaction entity
            transaction.setFromAccountId(sourceAccount.getAccountId());
            transaction.setToAccountId(targetAccount.getAccountId());
            transaction = transactionRepository.save(transaction); // Save again with resolved IDs

            // Add metadata: account-based transfer (from -> to)
            try {
                java.util.Map<String, String> meta = new java.util.HashMap<>();
                meta.put("method", "ACCOUNT");
                if (sourceAccount.getAccountNumber() != null) {
                    meta.put("fromAccountNumber", sourceAccount.getAccountNumber());
                    meta.put("fromAccountMasked", maskAccount(sourceAccount.getAccountNumber()));
                }
                if (targetAccount.getAccountNumber() != null) {
                    meta.put("toAccountNumber", targetAccount.getAccountNumber());
                    meta.put("toAccountMasked", maskAccount(targetAccount.getAccountNumber()));
                }
                transaction.setMetadataJson(new ObjectMapper().writeValueAsString(meta));
            } catch (Exception ignore) {}

            // Perform KYC check for both source and target users
            checkKycStatus(sourceAccount.getUserId());
            checkKycStatus(targetAccount.getUserId());

            // Enforce per-transaction limits on source (withdraw side)
            if (sourceAccount.getAccountType() == AccountDto.AccountType.SAVINGS) {
                if (request.getAmount() > 200000) {
                    throw new InvalidTransactionException("Transfer exceeds maximum per-transaction limit of 200000 INR for SAVINGS account.");
                }
            } else if (sourceAccount.getAccountType() == AccountDto.AccountType.SALARY_CORPORATE) {
                if (request.getAmount() > 500000) {
                    throw new InvalidTransactionException("Transfer exceeds maximum per-transaction limit of 500000 INR for SALARY/CORPORATE account.");
                }
            }

            // Enforce deposit-side limits on target and salary cap
            if (targetAccount.getAccountType() == AccountDto.AccountType.SAVINGS) {
                if (request.getAmount() > 200000) {
                    throw new InvalidTransactionException("Transfer exceeds maximum per-transaction deposit limit of 200000 INR for target SAVINGS account.");
                }
            } else if (targetAccount.getAccountType() == AccountDto.AccountType.SALARY_CORPORATE) {
                if (request.getAmount() > 500000) {
                    throw new InvalidTransactionException("Transfer exceeds maximum per-transaction deposit limit of 500000 INR for target SALARY/CORPORATE account.");
                }
                if (targetAccount.getBalance() + request.getAmount() > 10000000) {
                    throw new InvalidTransactionException("Transfer would exceed the maximum balance cap of 10000000 INR for target SALARY/CORPORATE account.");
                }
            }

            if (sourceAccount.getBalance() < request.getAmount()) {
                throw new InsufficientFundsException("Insufficient funds in source account: " + request.getFromAccountNumber());
            }

            // OTP verification before proceeding
            OtpVerifyRequest otpReq = new OtpVerifyRequest(
                sourceAccount.getUserId(),
                "WITHDRAWAL",
                null,
                request.getOtpCode()
            );
            OtpVerifyResponse otpRes = otpServiceClient.verify(otpReq);
            if (otpRes == null || !otpRes.isVerified()) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                throw new TransactionProcessingException("OTP verification failed: " + (otpRes != null ? otpRes.getMessage() : "no response"));
            }

            if (sourceAccount.getAccountId().equals(targetAccount.getAccountId())) { // Compare resolved IDs
                throw new InvalidTransactionException("Cannot transfer funds to the same account.");
            }

            WithdrawRequestDto withdrawRequestDto = new WithdrawRequestDto(transaction.getTransactionId(), request.getAmount());
            accountServiceClient.withdrawFunds(sourceAccount.getAccountId(), withdrawRequestDto); // Use resolved ID

            DepositRequestDto depositRequestDto = new DepositRequestDto(transaction.getTransactionId(), request.getAmount());
            accountServiceClient.depositFunds(targetAccount.getAccountId(), depositRequestDto); // Use resolved ID

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction = transactionRepository.save(transaction);

            String senderNotificationMessage = "A transfer of " + request.getAmount() + " has been made from your account " + sourceAccount.getAccountNumber() + " to " + targetAccount.getAccountNumber() + ". Transaction ID: " + transaction.getTransactionId();
            publishTransactionCompletedEvent(
                transaction.getTransactionId(),
                sourceAccount.getUserId(),
                sourceAccount.getAccountId(),
                request.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                senderNotificationMessage
            );

            String receiverNotificationMessage = "You have received " + request.getAmount() + " in your account " + targetAccount.getAccountNumber() + " from " + sourceAccount.getAccountNumber() + ". Transaction ID: " + transaction.getTransactionId();
            publishTransactionCompletedEvent(
                transaction.getTransactionId(),
                targetAccount.getUserId(),
                targetAccount.getAccountId(),
                request.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                receiverNotificationMessage
            );

        } catch (HttpClientErrorException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Transfer failed due to Account Service error: " + e.getResponseBodyAsString(), e);
        } catch (AccountNotFoundException | InsufficientFundsException | InvalidTransactionException | UnauthorizedUserException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Transfer failed unexpectedly: " + e.getMessage(), e);
        }
        return transaction;
    }

    /**
     * Retrieves a transaction by its ID.
     */
    @Override
    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    /**
     * Retrieves all transactions for a given account ID (either as fromAccountId or toAccountId).
     */
    @Override
    public List<Transaction> getTransactionsByAccountId(String accountId) {
        // Ensure latest transactions appear first
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByTransactionDateDesc(accountId, accountId);
    }

    @Override
    @Transactional
    public Transaction recordFine(FineRequest request) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(null);
        transaction.setToAccountId(request.getAccountId());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.FINE);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        try {
            AccountDto account = accountServiceClient.getAccountById(request.getAccountId());
            String notificationMessage = (request.getMessage() != null && !request.getMessage().isBlank())
                ? request.getMessage()
                : ("A fine of INR " + request.getAmount() + " has been recorded for account " + (account != null ? account.getAccountNumber() : request.getAccountId()) + ". Transaction ID: " + transaction.getTransactionId());
            if (account != null) {
                publishTransactionCompletedEvent(
                    transaction.getTransactionId(),
                    account.getUserId(),
                    account.getAccountId(),
                    request.getAmount(),
                    transaction.getType().name(),
                    transaction.getStatus().name(),
                    notificationMessage
                );
            }
            return transaction;
        } catch (Exception e) {
            System.err.println("recordFine: notification publish failed: " + e.getMessage());
            return transaction;
        }
    }

    @Override
    @Transactional
    public Transaction internalDebit(InternalDebitRequest request) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(request.getAccountId());
        transaction.setToAccountId(null);
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.INTERNAL_DEBIT);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setReason(request.getReason());
        try {
            if (request.getMetadata() != null) {
                transaction.setMetadataJson(new ObjectMapper().writeValueAsString(request.getMetadata()));
            }
        } catch (Exception e) {
            transaction.setMetadataJson(null);
        }
        transaction = transactionRepository.save(transaction);

        try {
            AccountDto account = accountServiceClient.getAccountById(request.getAccountId());
            if (account == null) {
                throw new AccountNotFoundException("Account not found with ID: " + request.getAccountId());
            }
            // Ensure metadata for account-based internal debit if not provided
            if (transaction.getMetadataJson() == null || transaction.getMetadataJson().isBlank()) {
                try {
                    java.util.Map<String, String> meta = new java.util.HashMap<>();
                    meta.put("method", "ACCOUNT");
                    if (account.getAccountNumber() != null) {
                        meta.put("fromAccountNumber", account.getAccountNumber());
                        meta.put("fromAccountMasked", maskAccount(account.getAccountNumber()));
                    }
                    if (request.getReason() != null) {
                        meta.put("reason", request.getReason());
                    }
                    transaction.setMetadataJson(new ObjectMapper().writeValueAsString(meta));
                } catch (Exception ignore) {}
            }
            if (account.getBalance() < request.getAmount()) {
                throw new InsufficientFundsException("Insufficient funds in account: " + request.getAccountId());
            }

            WithdrawRequestDto withdrawRequestDto = new WithdrawRequestDto(transaction.getTransactionId(), request.getAmount());
            accountServiceClient.withdrawFunds(request.getAccountId(), withdrawRequestDto);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction = transactionRepository.save(transaction);

            String brand = request.getMetadata() != null ? request.getMetadata().get("brand") : null;
            String cardType = request.getMetadata() != null ? request.getMetadata().get("type") : null;
            String reason = request.getReason() != null ? request.getReason() : "internal operation";
            String suffix = (brand != null || cardType != null) ? (" (" + (cardType != null ? cardType : "") + (brand != null ? "-" + brand : "") + ")") : "";

            String notificationMessage = "An internal debit of INR " + request.getAmount()
                + " was applied to your account " + account.getAccountNumber()
                + " for " + reason + suffix + ". Transaction ID: " + transaction.getTransactionId();

            publishTransactionCompletedEvent(
                transaction.getTransactionId(),
                account.getUserId(),
                account.getAccountId(),
                request.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                notificationMessage
            );

        } catch (HttpClientErrorException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Internal debit failed due to Account Service error: " + e.getResponseBodyAsString(), e);
        } catch (AccountNotFoundException | InsufficientFundsException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Internal debit failed unexpectedly: " + e.getMessage(), e);
        }
        return transaction;
    }

    @Override
    @Transactional
    public Transaction debitCardWithdraw(DebitCardWithdrawRequest request) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(null);
        transaction.setToAccountId(null);
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setReason("DEBIT_CARD_WITHDRAW");
        try {
            java.util.Map<String, String> meta = new java.util.HashMap<>();
            meta.put("method", "DEBIT_CARD");
            transaction.setMetadataJson(new ObjectMapper().writeValueAsString(meta));
        } catch (Exception e) {
            transaction.setMetadataJson(null);
        }
        transaction = transactionRepository.save(transaction);

        try {
            // Validate card with CreditCardService
            DebitCardValidationResponse validation = creditCardServiceClient.validateDebitTransaction(
                new DebitCardValidationRequest(request.getCardNumber(), request.getCvv())
            );
            if (validation == null || !validation.isValid()) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                throw new TransactionProcessingException("Debit card validation failed: " + (validation != null ? validation.getMessage() : "no response"));
            }

            // Update transaction with resolved account and metadata
            transaction.setFromAccountId(validation.getAccountId());
            try {
                java.util.Map<String, String> meta2 = new java.util.HashMap<>();
                meta2.put("method", "DEBIT_CARD");
                if (validation.getBrand() != null) meta2.put("brand", validation.getBrand());
                if (validation.getMaskedPan() != null) meta2.put("panMasked", validation.getMaskedPan());
                transaction.setMetadataJson(new ObjectMapper().writeValueAsString(meta2));
            } catch (Exception ignore) {}
            transaction = transactionRepository.save(transaction);

            AccountDto sourceAccount = accountServiceClient.getAccountById(validation.getAccountId());
            if (sourceAccount == null) {
                throw new AccountNotFoundException("Source account not found: " + validation.getAccountId());
            }

            // Enforce per-transaction limits by account type
            if (sourceAccount.getAccountType() == AccountDto.AccountType.SAVINGS) {
                if (request.getAmount() > 200000) {
                    throw new InvalidTransactionException("Withdrawal exceeds maximum per-transaction limit of 200000 INR for SAVINGS account.");
                }
            } else if (sourceAccount.getAccountType() == AccountDto.AccountType.SALARY_CORPORATE) {
                if (request.getAmount() > 500000) {
                    throw new InvalidTransactionException("Withdrawal exceeds maximum per-transaction limit of 500000 INR for SALARY/CORPORATE account.");
                }
            }

            if (sourceAccount.getBalance() < request.getAmount()) {
                throw new InsufficientFundsException("Insufficient funds in account: " + validation.getAccountId());
            }

            // OTP verification before proceeding
            OtpVerifyRequest otpReq = new OtpVerifyRequest(
                validation.getUserId(),
                "WITHDRAWAL",
                null,
                request.getOtpCode()
            );
            OtpVerifyResponse otpRes = otpServiceClient.verify(otpReq);
            if (otpRes == null || !otpRes.isVerified()) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                throw new TransactionProcessingException("OTP verification failed: " + (otpRes != null ? otpRes.getMessage() : "no response"));
            }

            WithdrawRequestDto withdrawRequestDto = new WithdrawRequestDto(transaction.getTransactionId(), request.getAmount());
            accountServiceClient.withdrawFunds(validation.getAccountId(), withdrawRequestDto);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction = transactionRepository.save(transaction);

            String brand = validation.getBrand();
            String maskedPan = validation.getMaskedPan();
            StringBuilder usingPart = new StringBuilder();
            if (brand != null || maskedPan != null) {
                usingPart.append(" using");
                if (brand != null) {
                    usingPart.append(" ").append(brand);
                }
                usingPart.append(" card");
                if (maskedPan != null) {
                    usingPart.append(" ").append(maskedPan);
                }
            }

            String notificationMessage = "A debit-card withdrawal of " + request.getAmount()
                + " has been made from your account " + sourceAccount.getAccountNumber()
                + usingPart.toString()
                + ". Transaction ID: " + transaction.getTransactionId();

            publishTransactionCompletedEvent(
                transaction.getTransactionId(),
                sourceAccount.getUserId(),
                sourceAccount.getAccountId(),
                request.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                notificationMessage
            );

        } catch (HttpClientErrorException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Debit-card withdrawal failed due to Account Service error: " + e.getResponseBodyAsString(), e);
        } catch (AccountNotFoundException | InsufficientFundsException | InvalidTransactionException e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Debit-card withdrawal failed unexpectedly: " + e.getMessage(), e);
        }

        return transaction;
    }

    // Helper to mask an account number, showing only last 4 digits
    private String maskAccount(String acc) {
        if (acc == null) return null;
        String digits = acc.replaceAll("\\s", "");
        int n = digits.length();
        if (n <= 4) return digits;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n - 4; i++) sb.append('•');
        sb.append(digits.substring(n - 4));
        return sb.toString();
    }

    @CircuitBreaker(name = "kafkaNotificationPublisher", fallbackMethod = "publishTransactionCompletedEventFallback")
    private void publishTransactionCompletedEvent(String transactionId, String userId, String accountId, Double amount, String type, String status, String notificationMessage) {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
            transactionId,
            userId,
            accountId,
            amount,
            type,
            status,
            notificationMessage
        );
        kafkaTemplate.send("transaction-events", event);
        System.out.println("Published transaction event to Kafka: " + event.getTransactionId());
    }

    private void publishTransactionCompletedEventFallback(String transactionId, String userId, String accountId, Double amount, String type, String status, String notificationMessage, Throwable t) {
        System.err.println("Fallback triggered for Kafka publishing for transaction " + transactionId + " due to: " + t.getMessage());
        System.err.println("Attempting to send notification directly via Notification Service Feign client.");

        try {
            NotificationRequestDto notificationRequest = new NotificationRequestDto(userId, NotificationRequestDto.NotificationType.EMAIL, "Fallback: " + notificationMessage);
            notificationServiceClient.sendEmailNotification(notificationRequest);
            System.out.println("Notification sent directly via Feign client for transaction: " + transactionId);
        } catch (Exception feignException) {
            System.err.println("Failed to send notification directly via Feign client for transaction " + transactionId + ": " + feignException.getMessage());
        }
    }
}
