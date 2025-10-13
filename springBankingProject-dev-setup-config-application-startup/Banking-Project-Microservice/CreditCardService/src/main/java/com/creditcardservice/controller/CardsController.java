package com.creditcardservice.controller;

import com.creditcardservice.dto.CardApplicationResponse;
import com.creditcardservice.dto.CardResponse;
import com.creditcardservice.dto.CreateCardApplicationRequest;
import com.creditcardservice.dto.ReviewCardApplicationRequest;
import com.creditcardservice.dto.ValidateDebitCardRequest;
import com.creditcardservice.dto.ValidateDebitCardResponse;
import com.creditcardservice.dto.RevealPanRequest;
import com.creditcardservice.dto.RevealPanResponse;
import com.creditcardservice.dto.RegenerateCvvRequest;
import com.creditcardservice.dto.RegenerateCvvResponse;
import com.creditcardservice.dto.FeeResponse;
import com.creditcardservice.model.CardApplication.ApplicationStatus;
import com.creditcardservice.service.CardsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * New Cards controller supporting application-based issuance for credit and debit cards.
 * User endpoints require authentication; admin endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/cards")
public class CardsController {

    @Autowired
    private CardsService cardsService;

    // USER

    @PostMapping("/applications")
    public ResponseEntity<CardApplicationResponse> submitApplication(@Valid @RequestBody CreateCardApplicationRequest request) {
        CardApplicationResponse resp = cardsService.submitApplication(request);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping("/applications/mine")
    public ResponseEntity<List<CardApplicationResponse>> listMyApplications(@RequestParam String userId) {
        List<CardApplicationResponse> list = cardsService.listMyApplications(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CardResponse>> listMyCards(@RequestParam String userId) {
        List<CardResponse> list = cardsService.listMyCards(userId);
        return ResponseEntity.ok(list);
    }

    // Debit card transaction validation (for withdrawals via TransactionService)
    @PostMapping("/debit/validate-transaction")
    public ResponseEntity<ValidateDebitCardResponse> validateDebit(@Valid @RequestBody ValidateDebitCardRequest request) {
        ValidateDebitCardResponse resp = cardsService.validateDebitCard(request);
        return ResponseEntity.ok(resp);
    }

    // Reveal full PAN for a user's own DEBIT card after OTP verification
    @PostMapping("/{id}/reveal-pan")
    public ResponseEntity<RevealPanResponse> revealPan(
            @PathVariable("id") String id,
            @Valid @RequestBody RevealPanRequest request) {
        RevealPanResponse resp = cardsService.revealPan(id, request);
        return ResponseEntity.ok(resp);
    }

    // Regenerate CVV for a user's own DEBIT card after OTP verification (returns one-time plaintext)
    @PostMapping("/{id}/regenerate-cvv")
    public ResponseEntity<RegenerateCvvResponse> regenerateCvv(
            @PathVariable("id") String id,
            @Valid @RequestBody RegenerateCvvRequest request) {
        RegenerateCvvResponse resp = cardsService.regenerateCvv(id, request);
        return ResponseEntity.ok(resp);
    }

    // ADMIN

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CardApplicationResponse>> listApplicationsByStatus(
            @RequestParam(name = "status", required = false, defaultValue = "SUBMITTED") String status) {
        ApplicationStatus st = ApplicationStatus.valueOf(status.toUpperCase());
        List<CardApplicationResponse> list = cardsService.listApplicationsByStatus(st);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardApplicationResponse> getApplication(@PathVariable("id") String id) {
        return ResponseEntity.ok(cardsService.getApplication(id));
    }

    @PutMapping("/applications/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardApplicationResponse> reviewApplication(
            @PathVariable("id") String id,
            @Valid @RequestBody ReviewCardApplicationRequest request) {
        CardApplicationResponse resp = cardsService.reviewApplication(id, request);
        return ResponseEntity.ok(resp);
    }

    // FEES: expose issuance fee based on account type and card kind for UI display
    // Example: GET /cards/fees?accountType=SAVINGS&kind=DEBIT
    @GetMapping("/fees")
    public ResponseEntity<FeeResponse> getIssuanceFee(@RequestParam String accountType,
                                                      @RequestParam String kind) {
        FeeResponse fee = cardsService.getIssuanceFee(accountType, kind);
        return ResponseEntity.ok(fee);
    }
}
