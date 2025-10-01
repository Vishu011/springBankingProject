package com.omnibank.accountmanagement.application;

import com.omnibank.accountmanagement.config.AppProperties;
import com.omnibank.accountmanagement.domain.Account;
import com.omnibank.accountmanagement.events.EventPublisher;
import com.omnibank.accountmanagement.events.EventTypes;
import com.omnibank.accountmanagement.repository.AccountRepository;
import com.omnibank.accountmanagement.api.dto.LedgerTransactionPostedDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountManagementService {

  private final AccountRepository accountRepository;
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  @Transactional
  public String createAccount(Long customerId, String accountType, String correlationId) {
    if (customerId == null || customerId <= 0) {
      throw new IllegalArgumentException("customerId must be provided");
    }
    if (accountType == null || accountType.isBlank()) {
      throw new IllegalArgumentException("accountType must be provided");
    }
    String accountNumber = generateAccountNumber();
    Account account = Account.builder()
        .accountNumber(accountNumber)
        .customerId(customerId)
        .accountType(accountType.toUpperCase())
        .status("ACTIVE")
        .balance(BigDecimal.ZERO)
        .openingDate(Instant.now())
        .build();
    accountRepository.save(account);

    publish(EventTypes.ACCOUNT_CREATED,
        new EventPayloads.AccountCreated(account.getAccountNumber(), account.getCustomerId(), account.getAccountType(), account.getStatus()),
        correlationId);

    return accountNumber;
  }

  @Transactional(readOnly = true)
  public List<Account> listCustomerAccounts(Long customerId) {
    return accountRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
  }

  @Transactional(readOnly = true)
  public BigDecimal getBalance(String accountNumber) {
    Account account = accountRepository.findById(accountNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
    return account.getBalance();
  }

  /**
   * Dev-only adjustment to simulate credits/debits; positive = credit, negative = debit.
   */
  @Transactional
  public BigDecimal adjustBalanceDev(String accountNumber, BigDecimal amount, String correlationId) {
    if (amount == null) throw new IllegalArgumentException("amount is required");
    Account account = accountRepository.findById(accountNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

    BigDecimal newBalance = account.getBalance().add(amount);
    account.setBalance(newBalance);
    accountRepository.save(account);

    publish(EventTypes.BALANCE_UPDATED,
        new EventPayloads.BalanceUpdated(account.getAccountNumber(), account.getCustomerId(), newBalance),
        correlationId);

    return newBalance;
  }

  /**
   * Dev-only webhook to apply a TransactionPosted from the ledger (simulating event consumption).
   * For each entry: D (debit) subtracts amount, C (credit) adds amount.
   */
  @Transactional
  public void applyLedgerTransactionPosted(LedgerTransactionPostedDto payload, String correlationId) {
    if (payload == null || payload.getEntries() == null || payload.getEntries().isEmpty()) {
      throw new IllegalArgumentException("entries are required");
    }
    for (var e : payload.getEntries()) {
      if (e.getAccount() == null || e.getAccount().isBlank()) {
        throw new IllegalArgumentException("entry.account is required");
      }
      if (e.getAmount() == null) {
        throw new IllegalArgumentException("entry.amount is required");
      }
      Account account = accountRepository.findById(e.getAccount())
          .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + e.getAccount()));

      boolean isDebit = e.getDirection() != null && Character.toUpperCase(e.getDirection()) == 'D';
      var delta = isDebit ? e.getAmount().negate() : e.getAmount();
      account.setBalance(account.getBalance().add(delta));
      accountRepository.save(account);

      publish(EventTypes.BALANCE_UPDATED,
          new EventPayloads.BalanceUpdated(account.getAccountNumber(), account.getCustomerId(), account.getBalance()),
          correlationId);
    }
  }

  private void publish(String type, Object payload, String correlationId) {
    eventPublisher.publish(props.getEvents().getTopic(), type, payload, correlationId);
  }

  private static String generateAccountNumber() {
    // Simple unique-ish generator: AC + epoch seconds + 4 random digits (trim to max 30 length)
    String base = "AC" + (System.currentTimeMillis() / 1000L) + String.format("%04d", new Random().nextInt(10000));
    return base.length() <= 30 ? base : base.substring(0, 30);
  }

  public static class EventPayloads {
    public record AccountCreated(String accountNumber, Long customerId, String accountType, String status) {}
    public record BalanceUpdated(String accountNumber, Long customerId, java.math.BigDecimal balance) {}
  }

  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
  }
}
