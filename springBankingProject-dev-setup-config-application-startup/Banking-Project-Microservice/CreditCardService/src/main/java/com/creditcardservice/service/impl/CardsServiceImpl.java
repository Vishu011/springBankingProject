package com.creditcardservice.service.impl;

import com.creditcardservice.dao.CardApplicationRepository;
import com.creditcardservice.dao.CardRepository;
import com.creditcardservice.dto.*;
import com.creditcardservice.dto.AccountDto.AccountType;
import com.creditcardservice.exceptions.CardServiceException;
import com.creditcardservice.model.Card;
import com.creditcardservice.model.CardApplication;
import com.creditcardservice.model.CardApplication.ApplicationStatus;
import com.creditcardservice.model.CardBrand;
import com.creditcardservice.model.CardKind;
import com.creditcardservice.model.CardStatus;
import com.creditcardservice.proxyservice.AccountServiceClient;
import com.creditcardservice.proxyservice.InternalTransactionClient;
import com.creditcardservice.proxyservice.OtpServiceClient;
import com.creditcardservice.proxyservice.NotificationServiceClient;
import com.creditcardservice.service.CardsService;
import com.creditcardservice.util.CardNumberUtil;
import com.creditcardservice.util.CvvUtil;
import com.creditcardservice.util.HashUtil;
import com.creditcardservice.dto.FeeResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CardsServiceImpl implements CardsService {

    private static final Logger log = LoggerFactory.getLogger(CardsServiceImpl.class);

    private static final double SAVINGS_DEBIT_FEE = 200.0;
    private static final double SAVINGS_CREDIT_FEE = 500.0;
    private static final double CORPORATE_DEBIT_FEE = 0.0;
    private static final double CORPORATE_CREDIT_FEE = 750.0;

    // Simple in-memory throttling for PAN reveal: max 5 attempts per 10 minutes per (userId, cardId)
    private static final int REVEAL_MAX_ATTEMPTS = 5;
    private static final long REVEAL_WINDOW_SECONDS = 600L; // 10 minutes
    private final java.util.Map<String, java.util.Deque<java.time.Instant>> revealAttemptWindow =
            new java.util.concurrent.ConcurrentHashMap<>();

    // Rate limiting for CVV regeneration: max 3 attempts per 30 minutes per (userId, cardId)
    private static final int REGEN_CVV_MAX_ATTEMPTS = 3;
    private static final long REGEN_CVV_WINDOW_SECONDS = 1800L; // 30 minutes
    private final java.util.Map<String, java.util.Deque<java.time.Instant>> regenCvvAttemptWindow =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private CardApplicationRepository applicationRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private OtpServiceClient otpServiceClient;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    private InternalTransactionClient internalTransactionClient;

    // USER

    @Override
    @Transactional
    public CardApplicationResponse submitApplication(CreateCardApplicationRequest request) {
        // Verify OTP for card issuance
        OtpVerifyResponse otpRes = otpServiceClient.verify(
                new OtpVerifyRequest(request.getUserId(), "CARD_ISSUANCE", request.getAccountId(), request.getOtpCode()));
        if (otpRes == null || !otpRes.isVerified()) {
            throw new CardServiceException("OTP verification failed" + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
        }

        // Validate account ownership and status
        AccountDto account = accountServiceClient.getAccountById(request.getAccountId());
        if (account == null) {
            throw new CardServiceException("Account not found");
        }
        if (!Objects.equals(account.getUserId(), request.getUserId())) {
            throw new CardServiceException("Account does not belong to user");
        }
        if (account.getStatus() == null || !"ACTIVE".equalsIgnoreCase(account.getStatus().name())) {
            throw new CardServiceException("Account must be ACTIVE");
        }

        // Brand eligibility by account type
        if (!isBrandAllowedForAccountType(request.getRequestedBrand(), account.getAccountType())) {
            throw new CardServiceException("Requested brand not allowed for account type");
        }

        // Enforce per-account limits on existing issued cards (pre-check)
        enforcePerAccountLimitsPreCheck(account.getAccountId(), account.getAccountType(), request.getType());

        // Persist application
        CardApplication app = CardApplication.builder()
                .userId(request.getUserId())
                .accountId(request.getAccountId())
                .type(request.getType())
                .requestedBrand(request.getRequestedBrand())
                .status(ApplicationStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        app = applicationRepository.save(app);
        // Notify user about application submission
        try {
            String content = "Your " + request.getType() + " card application for account " + account.getAccountId()
                    + " requesting " + request.getRequestedBrand() + " has been submitted and is pending admin review.";
            notificationServiceClient.sendEmailNotification(
                new NotificationRequestDto(request.getUserId(), NotificationRequestDto.NotificationType.EMAIL, content)
            );
        } catch (Exception e) {
            System.err.println("Notification failed (submission): " + e.getMessage());
        }
        return toApplicationResponse(app, null, null);
    }

    @Override
    public List<CardApplicationResponse> listMyApplications(String userId) {
        List<CardApplication> apps = applicationRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        List<CardApplicationResponse> out = new ArrayList<>();
        for (CardApplication app : apps) {
            out.add(toApplicationResponse(app, null, null));
        }
        return out;
    }

    @Override
    public List<CardResponse> listMyCards(String userId) {
        List<Card> cards = cardRepository.findByUserId(userId);
        List<CardResponse> out = new ArrayList<>();
        for (Card c : cards) {
            out.add(toCardResponse(c));
        }
        return out;
    }

    @Override
    public ValidateDebitCardResponse validateDebitCard(ValidateDebitCardRequest request) {
        try {
            Optional<Card> cardOpt = cardRepository.findByCardNumber(request.getCardNumber());
            if (cardOpt.isEmpty()) {
                return new ValidateDebitCardResponse(false, "Card not found", null, null, null, null, null, null);
            }
            Card card = cardOpt.get();

            if (card.getType() != CardKind.DEBIT) {
                return new ValidateDebitCardResponse(false, "Not a debit card", null, null, null, null, null, null);
            }
            if (card.getStatus() != CardStatus.ACTIVE) {
                return new ValidateDebitCardResponse(false, "Card not active", null, null, null, null, null, null);
            }

            // Expiry check (month/year only)
            LocalDate today = LocalDate.now();
            if (card.getExpiryYear() < today.getYear() || (card.getExpiryYear().equals(today.getYear()) && card.getExpiryMonth() < today.getMonthValue())) {
                return new ValidateDebitCardResponse(false, "Card expired", null, null, null, null, null, null);
            }

            // Account type based CVV length
            AccountDto account = accountServiceClient.getAccountById(card.getAccountId());
            if (account == null) {
                return new ValidateDebitCardResponse(false, "Linked account not found", null, null, null, null, null, null);
            }
            int expectedLen = cvvLengthFor(account.getAccountType(), card.getBrand());
            if (request.getCvv() == null || request.getCvv().length() != expectedLen) {
                return new ValidateDebitCardResponse(false, "Invalid CVV length", null, null, null, null, null, null);
            }

            // CVV hash check
            String providedHash = HashUtil.sha256(request.getCvv());
            if (!providedHash.equals(card.getCvvHash())) {
                return new ValidateDebitCardResponse(false, "Invalid CVV", null, null, null, null, null, null);
            }

            // Valid
            return new ValidateDebitCardResponse(
                true,
                "OK",
                card.getUserId(),
                card.getAccountId(),
                card.getBrand(),
                CardNumberUtil.maskPan(card.getCardNumber()),
                card.getExpiryMonth(),
                card.getExpiryYear()
            );
        } catch (Exception e) {
            return new ValidateDebitCardResponse(false, "Validation error: " + e.getMessage(), null, null, null, null, null, null);
        }
    }

    // Reveal full PAN for a user's own DEBIT card after OTP verification
    @Override
    public RevealPanResponse revealPan(String cardId, RevealPanRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()
                || request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new CardServiceException("userId and otpCode are required");
        }

        log.info("AUDIT RevealPAN attempt userId={} cardId={}", request.getUserId(), cardId);
        String rateKey = request.getUserId() + ":" + cardId;
        if (!allowRevealAttempt(rateKey)) {
            log.warn("AUDIT RevealPAN throttled userId={} cardId={}", request.getUserId(), cardId);
            throw new CardServiceException("Too many PAN reveal attempts. Please try again later.");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardServiceException("Card not found"));

        // Must belong to the requester
        if (!Objects.equals(card.getUserId(), request.getUserId())) {
            throw new CardServiceException("Card does not belong to user");
        }

        // Allow PAN reveal for both DEBIT and CREDIT with OTP; ensure card is not expired
        LocalDate today = LocalDate.now();
        if (card.getExpiryYear() < today.getYear() || (card.getExpiryYear().equals(today.getYear()) && card.getExpiryMonth() < today.getMonthValue())) {
            throw new CardServiceException("Card expired");
        }

        // Verify OTP for card operation (robust error handling to avoid 500s)
        OtpVerifyResponse otpRes;
        try {
            otpRes = otpServiceClient.verify(
                    new OtpVerifyRequest(request.getUserId(), "CARD_OPERATION", card.getAccountId(), request.getOtpCode()));
        } catch (feign.FeignException fe) {
            log.warn("AUDIT RevealPAN OTP verify feign error userId={} cardId={} status={} msg={}", request.getUserId(), card.getCardId(), fe.status(), fe.getMessage());
            if (fe.status() >= 400 && fe.status() < 500) {
                throw new CardServiceException("OTP verification failed - downstream error " + fe.status());
            }
            throw new CardServiceException("OTP verification service unavailable");
        } catch (Exception ex) {
            log.warn("AUDIT RevealPAN OTP verify error userId={} cardId={} msg={}", request.getUserId(), card.getCardId(), ex.getMessage());
            throw new CardServiceException("OTP verification failed");
        }
        if (otpRes == null || !otpRes.isVerified()) {
            log.warn("AUDIT RevealPAN failed OTP userId={} cardId={} msg={}", request.getUserId(), card.getCardId(), (otpRes != null ? otpRes.getMessage() : "null"));
            throw new CardServiceException("OTP verification failed" + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
        }

        // Return full PAN
        log.info("AUDIT RevealPAN success userId={} cardId={}", request.getUserId(), card.getCardId());
        return new RevealPanResponse(card.getCardId(), card.getCardNumber(), "PAN revealed after OTP verification");
    }

    // Regenerate CVV for a user's own DEBIT card after OTP verification
    @Override
    public RegenerateCvvResponse regenerateCvv(String cardId, RegenerateCvvRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()
                || request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new CardServiceException("userId and otpCode are required");
        }

        log.info("AUDIT RegenerateCVV attempt userId={} cardId={}", request.getUserId(), cardId);

        // Throttle attempts per (userId, cardId)
        String rateKey = request.getUserId() + ":" + cardId;
        java.time.Instant now = java.time.Instant.now();
        regenCvvAttemptWindow.computeIfAbsent(rateKey, k -> new java.util.ArrayDeque<>());
        java.util.Deque<java.time.Instant> dq = regenCvvAttemptWindow.get(rateKey);
        synchronized (dq) {
            while (!dq.isEmpty() && java.time.Duration.between(dq.peekFirst(), now).getSeconds() > REGEN_CVV_WINDOW_SECONDS) {
                dq.pollFirst();
            }
            if (dq.size() >= REGEN_CVV_MAX_ATTEMPTS) {
                log.warn("AUDIT RegenerateCVV throttled userId={} cardId={}", request.getUserId(), cardId);
                throw new CardServiceException("Too many CVV regeneration attempts. Please try again later.");
            }
            dq.addLast(now);
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardServiceException("Card not found"));

        // Must belong to the requester
        if (!Objects.equals(card.getUserId(), request.getUserId())) {
            throw new CardServiceException("Card does not belong to user");
        }

        // Only allow for DEBIT cards (credit cards are excluded by requirement)
        if (card.getType() != CardKind.DEBIT) {
            throw new CardServiceException("CVV regeneration is only allowed for DEBIT cards");
        }

        // Card must be ACTIVE
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardServiceException("Card not active");
        }

        // Expiry check (month/year only)
        LocalDate today = LocalDate.now();
        if (card.getExpiryYear() < today.getYear() || (card.getExpiryYear().equals(today.getYear()) && card.getExpiryMonth() < today.getMonthValue())) {
            throw new CardServiceException("Card expired");
        }

        // Determine CVV length by account type and brand
        AccountDto account = accountServiceClient.getAccountById(card.getAccountId());
        if (account == null) {
            throw new CardServiceException("Linked account not found");
        }
        int cvvLength = cvvLengthFor(account.getAccountType(), card.getBrand());

        // Verify OTP for card operation (robust error handling to avoid 500s)
        OtpVerifyResponse otpRes;
        try {
            otpRes = otpServiceClient.verify(
                    new OtpVerifyRequest(request.getUserId(), "CARD_OPERATION", card.getAccountId(), request.getOtpCode()));
        } catch (feign.FeignException fe) {
            log.warn("AUDIT RegenerateCVV OTP verify feign error userId={} cardId={} status={} msg={}", request.getUserId(), card.getCardId(), fe.status(), fe.getMessage());
            if (fe.status() >= 400 && fe.status() < 500) {
                throw new CardServiceException("OTP verification failed - downstream error " + fe.status());
            }
            throw new CardServiceException("OTP verification service unavailable");
        } catch (Exception ex) {
            log.warn("AUDIT RegenerateCVV OTP verify error userId={} cardId={} msg={}", request.getUserId(), card.getCardId(), ex.getMessage());
            throw new CardServiceException("OTP verification failed");
        }
        if (otpRes == null || !otpRes.isVerified()) {
            log.warn("AUDIT RegenerateCVV failed OTP userId={} cardId={} msg={}", request.getUserId(), card.getCardId(), (otpRes != null ? otpRes.getMessage() : "null"));
            throw new CardServiceException("OTP verification failed" + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
        }

        // Generate new CVV and persist its hash
        String newCvv = CvvUtil.generateCvv(cvvLength);
        String newHash = HashUtil.sha256(newCvv);
        card.setCvvHash(newHash);
        cardRepository.save(card);

        // Notify user (no plaintext CVV in notifications)
        try {
            String content = "Your card CVV has been regenerated for card ending " + CardNumberUtil.maskPan(card.getCardNumber())
                    + ". If you did not initiate this, contact support immediately.";
            notificationServiceClient.sendEmailNotification(
                    new NotificationRequestDto(card.getUserId(), NotificationRequestDto.NotificationType.EMAIL, content)
            );
        } catch (Exception e) {
            System.err.println("Notification failed (cvv regen): " + e.getMessage());
        }

        log.info("AUDIT RegenerateCVV success userId={} cardId={}", request.getUserId(), card.getCardId());
        // Return plaintext CVV once in response
        return new RegenerateCvvResponse(card.getCardId(), newCvv, "CVV regenerated successfully. Displayed once.");
    }

    private boolean allowRevealAttempt(String key) {
        java.time.Instant now = java.time.Instant.now();
        revealAttemptWindow.computeIfAbsent(key, k -> new java.util.ArrayDeque<>());
        java.util.Deque<java.time.Instant> dq = revealAttemptWindow.get(key);
        synchronized (dq) {
            // prune old entries outside window
            while (!dq.isEmpty() && java.time.Duration.between(dq.peekFirst(), now).getSeconds() > REVEAL_WINDOW_SECONDS) {
                dq.pollFirst();
            }
            if (dq.size() >= REVEAL_MAX_ATTEMPTS) {
                return false;
            }
            dq.addLast(now);
            return true;
        }
    }

    // ADMIN

    @Override
    public List<CardApplicationResponse> listApplicationsByStatus(ApplicationStatus status) {
        List<CardApplication> apps = applicationRepository.findByStatusOrderBySubmittedAtAsc(status);
        List<CardApplicationResponse> out = new ArrayList<>();
        for (CardApplication app : apps) {
            out.add(toApplicationResponse(app, null, null));
        }
        return out;
    }

    @Override
    public CardApplicationResponse getApplication(String applicationId) {
        CardApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CardServiceException("Application not found"));
        return toApplicationResponse(app, null, null);
    }

    @Override
    @Transactional
    public CardApplicationResponse reviewApplication(String applicationId, ReviewCardApplicationRequest request) {
        CardApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CardServiceException("Application not found"));

        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new CardServiceException("Only SUBMITTED applications can be reviewed");
        }

        app.setReviewerId(request.getReviewerId());
        app.setReviewedAt(LocalDateTime.now());
        app.setAdminComment(request.getAdminComment());

        if (request.getDecision() == ReviewCardApplicationRequest.Decision.REJECTED) {
            app.setStatus(ApplicationStatus.REJECTED);
            app = applicationRepository.save(app);
            // Notify user about rejection
            try {
                String content = "Your " + app.getType() + " card application for account " + app.getAccountId()
                        + " was rejected." + (app.getAdminComment() != null ? " Reason: " + app.getAdminComment() : "");
                notificationServiceClient.sendEmailNotification(
                    new NotificationRequestDto(app.getUserId(), NotificationRequestDto.NotificationType.EMAIL, content)
                );
            } catch (Exception e) {
                System.err.println("Notification failed (rejection): " + e.getMessage());
            }
            return toApplicationResponse(app, null, null);
        }

        // APPROVAL flow
        // Validate account again and enforce limits (approval-time check)
        AccountDto account = accountServiceClient.getAccountById(app.getAccountId());
        if (account == null) {
            throw new CardServiceException("Account not found");
        }
        if (!isBrandAllowedForAccountType(app.getRequestedBrand(), account.getAccountType())) {
            throw new CardServiceException("Requested brand not allowed for account type");
        }

        enforcePerAccountLimitsApprovalCheck(account.getAccountId(), account.getAccountType(), app.getType());

        // Dates: issue now (month/year), expiry default +5 years (month/year) unless overridden
        LocalDate now = LocalDate.now();
        int issueMonth = now.getMonthValue();
        int issueYear = now.getYear();

        int expiryMonth = request.getExpiryMonth() != null ? request.getExpiryMonth() : issueMonth;
        int expiryYear = request.getExpiryYear() != null ? request.getExpiryYear() : issueYear + 5;

        // Approved limit required for CREDIT, ignored for DEBIT
        Double approvedLimit = null;
        if (app.getType() == CardKind.CREDIT) {
            if (request.getApprovedLimit() == null || request.getApprovedLimit() <= 0) {
                throw new CardServiceException("approvedLimit is required and must be > 0 for CREDIT cards");
            }
            approvedLimit = request.getApprovedLimit();
        }

        // Generate PAN and CVV
        String pan = CardNumberUtil.generatePan(app.getRequestedBrand().name());
        // Ensure unique PAN
        int attempts = 0;
        while (cardRepository.existsByCardNumber(pan)) {
            pan = CardNumberUtil.generatePan(app.getRequestedBrand().name());
            attempts++;
            if (attempts > 10) {
                throw new CardServiceException("Failed to generate unique card number");
            }
        }

        int cvvLength = cvvLengthFor(account.getAccountType(), app.getRequestedBrand());
        String oneTimeCvv = CvvUtil.generateCvv(cvvLength);
        String cvvHash = HashUtil.sha256(oneTimeCvv);

        // Create Card
        Card card = Card.builder()
                .userId(app.getUserId())
                .accountId(app.getAccountId())
                .type(app.getType())
                .brand(app.getRequestedBrand())
                .cardNumber(pan)
                .cvvHash(cvvHash)
                .issueMonth(issueMonth)
                .issueYear(issueYear)
                .expiryMonth(expiryMonth)
                .expiryYear(expiryYear)
                .creditLimit(approvedLimit)
                .status(CardStatus.ACTIVE)
                .build();
        card = cardRepository.save(card);

        // Set application approval fields
        app.setIssueMonth(issueMonth);
        app.setIssueYear(issueYear);
        app.setExpiryMonth(expiryMonth);
        app.setExpiryYear(expiryYear);
        app.setApprovedLimit(approvedLimit);
        app.setGeneratedCardNumber(pan);
        app.setGeneratedCvvMasked(CvvUtil.maskCvv(oneTimeCvv));
        app.setStatus(ApplicationStatus.APPROVED);
        app = applicationRepository.save(app);

        // Debit issuance fee as per rules AFTER approval
        double fee = issuanceFee(account.getAccountType(), app.getType());
        if (fee > 0) {
            try {
                Map<String, String> meta = new HashMap<>();
                meta.put("type", app.getType().name());
                meta.put("brand", app.getRequestedBrand().name());
                internalTransactionClient.debit(new InternalDebitRequest(
                        app.getAccountId(), fee, "CARD_ISSUANCE_FEE", meta
                ));
            } catch (Exception ex) {
                // Rollback occurs due to @Transactional when exception propagates
                try {
                    String msg = "Card issuance approval failed due to fee debit error. No card has been issued. Reason: " + ex.getMessage();
                    notificationServiceClient.sendEmailNotification(
                        new NotificationRequestDto(app.getUserId(), NotificationRequestDto.NotificationType.EMAIL, msg)
                    );
                } catch (Exception notifyEx) {
                    System.err.println("Notification failed (fee failure): " + notifyEx.getMessage());
                }
                throw new CardServiceException("Issuance fee debit failed: " + ex.getMessage());
            }
        }

        // Notify user about approval and fee status
        try {
            String maskedPan = CardNumberUtil.maskPan(pan);
            String expiry = String.format("%02d/%02d", expiryMonth, (expiryYear % 100));
            String content = "Your " + app.getType() + " " + app.getRequestedBrand() + " card has been issued. Card: " + maskedPan + ", Expiry: " + expiry + ".";
            notificationServiceClient.sendEmailNotification(
                new NotificationRequestDto(app.getUserId(), NotificationRequestDto.NotificationType.EMAIL, content)
            );
            if (fee > 0) {
                String feeMsg = "Issuance fee of INR " + fee + " has been debited from your account " + account.getAccountNumber() + ".";
                notificationServiceClient.sendEmailNotification(
                    new NotificationRequestDto(app.getUserId(), NotificationRequestDto.NotificationType.EMAIL, feeMsg)
                );
            }
        } catch (Exception e) {
            System.err.println("Notification failed (approval): " + e.getMessage());
        }

        // Map response including oneTimeCvv shown once to admin
        CardApplicationResponse resp = toApplicationResponse(app, pan, oneTimeCvv);
        return resp;
    }

    // Helpers and mapping

    private boolean isBrandAllowedForAccountType(CardBrand brand, AccountType accountType) {
        if (accountType == AccountType.SALARY_CORPORATE) {
            return brand == CardBrand.AMEX || brand == CardBrand.MASTERCARD || brand == CardBrand.DISCOVERY;
        } else {
            // SAVINGS
            return brand == CardBrand.VISA || brand == CardBrand.RUPAY;
        }
    }

    private int cvvLengthFor(AccountType accountType, CardBrand brand) {
        if (accountType == AccountType.SALARY_CORPORATE) {
            return 4; // premium brands 4-digit
        }
        return 3; // savings 3-digit
    }

    private void enforcePerAccountLimitsPreCheck(String accountId, AccountType accountType, CardKind type) {
        // Check issued cards counts (pre-check gives early feedback)
        long existing = cardRepository.countByAccountIdAndType(accountId, type);
        int max = maxAllowed(accountType, type);
        if (existing >= max) {
            throw new CardServiceException("Per-account " + type + " card limit reached");
        }
        // Also count applications in progress to avoid spamming (SUBMITTED/APPROVED)
        long pendingOrApproved = applicationRepository.countByAccountIdAndTypeAndStatusIn(
                accountId, type, Arrays.asList(ApplicationStatus.SUBMITTED, ApplicationStatus.APPROVED));
        if (pendingOrApproved >= max) {
            throw new CardServiceException("Per-account " + type + " card limit already reached across applications/issued");
        }
    }

    private void enforcePerAccountLimitsApprovalCheck(String accountId, AccountType accountType, CardKind type) {
        // Count actual cards on approval to be strict
        long existing = cardRepository.countByAccountIdAndType(accountId, type);
        int max = maxAllowed(accountType, type);
        if (existing >= max) {
            throw new CardServiceException("Per-account " + type + " card limit reached (approval blocked)");
        }
    }

    private int maxAllowed(AccountType accountType, CardKind type) {
        // Savings: max 1 credit, max 2 debit.
        // Salary/Corporate: max 2 credit, max 2 debit.
        if (accountType == AccountType.SALARY_CORPORATE) {
            return 2;
        } else {
            if (type == CardKind.CREDIT) return 1;
            return 2;
        }
    }

    private double issuanceFee(AccountType accountType, CardKind kind) {
        if (accountType == AccountType.SALARY_CORPORATE) {
            return kind == CardKind.CREDIT ? CORPORATE_CREDIT_FEE : CORPORATE_DEBIT_FEE;
        } else {
            return kind == CardKind.CREDIT ? SAVINGS_CREDIT_FEE : SAVINGS_DEBIT_FEE;
        }
    }

    private CardApplicationResponse toApplicationResponse(CardApplication app, String pan, String oneTimeCvv) {
        CardApplicationResponse resp = new CardApplicationResponse();
        resp.setApplicationId(app.getApplicationId());
        resp.setUserId(app.getUserId());
        resp.setAccountId(app.getAccountId());
        resp.setType(app.getType());
        resp.setRequestedBrand(app.getRequestedBrand());
        resp.setStatus(app.getStatus());
        resp.setIssueMonth(app.getIssueMonth());
        resp.setIssueYear(app.getIssueYear());
        resp.setExpiryMonth(app.getExpiryMonth());
        resp.setExpiryYear(app.getExpiryYear());
        resp.setApprovedLimit(app.getApprovedLimit());
        resp.setReviewerId(app.getReviewerId());
        resp.setAdminComment(app.getAdminComment());
        if (app.getSubmittedAt() != null) resp.setSubmittedAt(app.getSubmittedAt().toString());
        if (app.getReviewedAt() != null) resp.setReviewedAt(app.getReviewedAt().toString());
        if (pan != null) {
            resp.setMaskedPan(CardNumberUtil.maskPan(pan));
        } else if (app.getGeneratedCardNumber() != null) {
            resp.setMaskedPan(CardNumberUtil.maskPan(app.getGeneratedCardNumber()));
        }
        if (app.getGeneratedCvvMasked() != null) {
            resp.setMaskedCvv(app.getGeneratedCvvMasked());
        }
        if (oneTimeCvv != null) {
            resp.setOneTimeCvv(oneTimeCvv);
        }
        return resp;
    }

    @Override
    public FeeResponse getIssuanceFee(String accountType, String kind) {
        AccountType at = AccountType.valueOf(accountType.trim().toUpperCase());
        CardKind ck = CardKind.valueOf(kind.trim().toUpperCase());
        double fee = issuanceFee(at, ck);
        String desc = "Issuance fee for " + at + " " + ck + " card";
        return new FeeResponse(fee, "INR", desc);
    }

    private CardResponse toCardResponse(Card c) {
        CardResponse resp = new CardResponse();
        resp.setCardId(c.getCardId());
        resp.setUserId(c.getUserId());
        resp.setAccountId(c.getAccountId());
        resp.setType(c.getType());
        resp.setBrand(c.getBrand());
        resp.setMaskedPan(CardNumberUtil.maskPan(c.getCardNumber()));
        // Populate masked CVV length appropriately (3 for savings, 4 for corporate premium brands)
        try {
            AccountDto acc = accountServiceClient.getAccountById(c.getAccountId());
            int len = (acc != null) ? cvvLengthFor(acc.getAccountType(), c.getBrand()) : 3;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) sb.append('*');
            resp.setMaskedCvv(sb.toString());
        } catch (Exception ignore) {
            resp.setMaskedCvv("***");
        }
        resp.setIssueMonth(c.getIssueMonth());
        resp.setIssueYear(c.getIssueYear());
        resp.setExpiryMonth(c.getExpiryMonth());
        resp.setExpiryYear(c.getExpiryYear());
        resp.setCreditLimit(c.getCreditLimit());
        resp.setStatus(c.getStatus());
        if (c.getCreatedAt() != null) resp.setCreatedAt(c.getCreatedAt().toString());
        return resp;
    }
}
