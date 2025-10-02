package com.omnibank.paymentgateway.application;

import com.omnibank.paymentgateway.api.dto.InternalTransferRequest;
import com.omnibank.paymentgateway.config.AppProperties;
import com.omnibank.paymentgateway.domain.PaymentRequest;
import com.omnibank.paymentgateway.domain.PaymentStatus;
import com.omnibank.paymentgateway.events.EventPublisher;
import com.omnibank.paymentgateway.events.EventTypes;
import com.omnibank.paymentgateway.integration.AccountManagementClient;
import com.omnibank.paymentgateway.integration.BeneficiaryManagementClient;
import com.omnibank.paymentgateway.integration.CustomerProfileClient;
import com.omnibank.paymentgateway.integration.FraudDetectionClient;
import com.omnibank.paymentgateway.integration.FraudDetectionClient.FraudRequest;
import com.omnibank.paymentgateway.integration.LedgerClient;
import com.omnibank.paymentgateway.repository.PaymentRequestRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

  private final PaymentRequestRepository repository;
  private final EventPublisher eventPublisher;
  private final AppProperties props;

  private final CustomerProfileClient customerProfileClient;
  private final AccountManagementClient accountManagementClient;
  private final BeneficiaryManagementClient beneficiaryManagementClient;
  private final FraudDetectionClient fraudDetectionClient;
  private final LedgerClient ledgerClient;

  // Idempotent variant: uses optional Idempotency-Key header to dedupe client retries
  @Transactional
  public InitiationResult initiateInternalTransfer(InternalTransferRequest req, String correlationId, String idempotencyKey) {
    String idKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : null;

    if (idKey != null) {
      var existing = repository.findByIdempotencyKey(idKey);
      if (existing.isPresent()) {
        var pr = existing.get();
        return new InitiationResult(pr.getPaymentUuid(), pr.getStatus());
      }
    }

    var result = initiateInternalTransfer(req, correlationId);

    if (idKey != null) {
      repository.findByPaymentUuid(result.paymentId()).ifPresent(pr -> {
        pr.setIdempotencyKey(idKey);
        repository.save(pr);
      });
    }

    return result;
  }

  /**
   * Initiate internal transfer. Returns paymentId and status (PROCESSING | MFA_CHALLENGE_REQUIRED | BLOCKED).
   */
  @Transactional
  public InitiationResult initiateInternalTransfer(InternalTransferRequest req, String correlationId) {
    validateBasic(req);

    // Ensure customer exists
    try {
      customerProfileClient.assertCustomerExists(req.getCustomerId(), correlationId);
    } catch (RestClientResponseException ex) {
      if (ex.getRawStatusCode() == 404) {
        throw new ResourceNotFoundException("Customer not found: " + req.getCustomerId());
      } else {
        throw new IllegalStateException("Customer profile check failed: HTTP " + ex.getRawStatusCode());
      }
    }

    // Ensure beneficiary exists and is ACTIVE for this customer
    final boolean active;
    try {
      active = beneficiaryManagementClient.isBeneficiaryActive(req.getCustomerId(), req.getToAccount(), correlationId);
    } catch (RestClientResponseException ex) {
      throw new IllegalStateException("Beneficiary check failed: HTTP " + ex.getRawStatusCode());
    }
    if (!active) {
      throw new IllegalStateException("Beneficiary not active or not found for customer");
    }

    // Funds check
    final BigDecimal balance;
    try {
      balance = accountManagementClient.getBalance(req.getFromAccount(), correlationId);
    } catch (RestClientResponseException ex) {
      if (ex.getRawStatusCode() == 404) {
        throw new ResourceNotFoundException("Account not found: " + req.getFromAccount());
      } else {
        throw new IllegalStateException("Account balance check failed: HTTP " + ex.getRawStatusCode());
      }
    }
    if (balance == null || balance.compareTo(req.getAmount()) < 0) {
      throw new IllegalStateException("Insufficient funds");
    }

    // Fraud hook (synchronous). Default ALLOW in dev if service unavailable.
    FraudDetectionClient.Decision decision =
        fraudDetectionClient.getDecision(FraudRequest.of(
            req.getCustomerId(),
            req.getFromAccount(),
            req.getToAccount(),
            req.getAmount(),
            req.getCurrency()
        ), correlationId);

    String paymentUuid = UUID.randomUUID().toString();
    PaymentRequest pr = PaymentRequest.builder()
        .paymentUuid(paymentUuid)
        .customerId(req.getCustomerId())
        .fromAccount(req.getFromAccount())
        .toAccount(req.getToAccount())
        .amount(req.getAmount())
        .currency(req.getCurrency().toUpperCase())
        .fraudScore(decision.score)
        .fraudActionTaken(decision.action)
        .build();

    // Decision orchestration
    String normalized = decision.action != null ? decision.action.toUpperCase() : "ALLOW";
    switch (normalized) {
      case "BLOCK" -> {
        pr.setStatus(PaymentStatus.BLOCKED.name());
        repository.save(pr);
        // Publish block event
        publish(EventTypes.FRAUDULENT_TRANSACTION_BLOCKED,
            new EventPayloads.FraudulentTransactionBlocked(
                pr.getPaymentUuid(),
                pr.getCustomerId(),
                pr.getFromAccount(),
                pr.getToAccount(),
                pr.getAmount(),
                pr.getCurrency(),
                decision.score
            ),
            correlationId);
        return new InitiationResult(pr.getPaymentUuid(), pr.getStatus());
      }
      case "CHALLENGE" -> {
        pr.setStatus(PaymentStatus.MFA_CHALLENGE_REQUIRED.name());
        repository.save(pr);
        // No processing event yet
        return new InitiationResult(pr.getPaymentUuid(), pr.getStatus());
      }
      default -> {
        // ALLOW -> proceed to processing and emit orchestration event
        pr.setStatus(PaymentStatus.PROCESSING.name());
        repository.save(pr);
        publish(EventTypes.PAYMENT_APPROVED_FOR_PROCESSING,
            new EventPayloads.PaymentApprovedForProcessing(
                pr.getPaymentUuid(),
                pr.getCustomerId(),
                pr.getFromAccount(),
                pr.getToAccount(),
                pr.getAmount(),
                pr.getCurrency()
            ),
            correlationId);

        // Short-circuit when devSyncPosting=false (tests/async mode): keep status PROCESSING
        if (!props.isDevSyncPosting()) {
          return new InitiationResult(pr.getPaymentUuid(), pr.getStatus());
        }

        // Dev-first completion: call ledger to post the double-entry
        try {
          var postRes = ledgerClient.postTransfer(
              pr.getFromAccount(),
              pr.getToAccount(),
              pr.getAmount(),
              pr.getCurrency(),
              correlationId
          );
          if (postRes != null && "POSTED".equalsIgnoreCase(postRes.status)) {
            if (props.isDevBalanceAdjustEnabled() && "logging".equalsIgnoreCase(props.getEventPublisher())) {
              try {
                // Dev-only: sync adjust balances in account-management to reflect the ledger posting
                accountManagementClient.adjustBalanceDev(pr.getFromAccount(), pr.getAmount().negate(), correlationId);
                accountManagementClient.adjustBalanceDev(pr.getToAccount(), pr.getAmount(), correlationId);
              } catch (Exception adjEx) {
                // If balance updates fail, still mark completed for flow; follow-up reconciliation may be needed
              }
            }
            pr.setStatus(PaymentStatus.COMPLETED.name());
          } else {
            pr.setStatus(PaymentStatus.REJECTED.name());
          }
        } catch (Exception ex) {
          // Any error from ledger -> mark as REJECTED for now (later: retry/CIRCUIT BREAK)
          pr.setStatus(PaymentStatus.REJECTED.name());
        }
        repository.save(pr);
        return new InitiationResult(pr.getPaymentUuid(), pr.getStatus());
      }
    }
  }

  @Transactional(readOnly = true)
  public String getStatusByUuid(String paymentUuid) {
    return repository.findByPaymentUuid(paymentUuid)
        .map(PaymentRequest::getStatus)
        .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentUuid));
  }

  private void publish(String type, Object payload, String correlationId) {
    eventPublisher.publish(props.getEvents().getTopic(), type, payload, correlationId);
  }

  private static void validateBasic(InternalTransferRequest req) {
    if (req.getCustomerId() == null || req.getCustomerId() <= 0) {
      throw new IllegalArgumentException("customerId must be provided");
    }
    if (req.getFromAccount() == null || req.getFromAccount().isBlank()) {
      throw new IllegalArgumentException("fromAccount must be provided");
    }
    if (req.getToAccount() == null || req.getToAccount().isBlank()) {
      throw new IllegalArgumentException("toAccount must be provided");
    }
    if (req.getFromAccount().equalsIgnoreCase(req.getToAccount())) {
      throw new IllegalArgumentException("fromAccount and toAccount cannot be the same");
    }
    if (req.getAmount() == null || req.getAmount().compareTo(new BigDecimal("0.00")) <= 0) {
      throw new IllegalArgumentException("amount must be greater than 0");
    }
    if (req.getCurrency() == null || req.getCurrency().isBlank()) {
      throw new IllegalArgumentException("currency must be provided");
    }
  }

  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
  }

  public record InitiationResult(String paymentId, String status) {}

  // Outbound event payloads
  public static class EventPayloads {
    public record PaymentApprovedForProcessing(
        String paymentUuid,
        Long customerId,
        String fromAccount,
        String toAccount,
        java.math.BigDecimal amount,
        String currency
    ) {}
    public record FraudulentTransactionBlocked(
        String paymentUuid,
        Long customerId,
        String fromAccount,
        String toAccount,
        java.math.BigDecimal amount,
        String currency,
        java.math.BigDecimal fraudScore
    ) {}
  }
}
