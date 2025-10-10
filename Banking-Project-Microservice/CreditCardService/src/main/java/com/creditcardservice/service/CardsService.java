package com.creditcardservice.service;

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
import com.creditcardservice.model.CardApplication.ApplicationStatus;

import java.util.List;

public interface CardsService {

    // USER
    CardApplicationResponse submitApplication(CreateCardApplicationRequest request);

    List<CardApplicationResponse> listMyApplications(String userId);

    List<CardResponse> listMyCards(String userId);

    // Debit-card transaction validation for withdrawals
    ValidateDebitCardResponse validateDebitCard(ValidateDebitCardRequest request);

    // Reveal full PAN for a user's own DEBIT card after OTP verification
    RevealPanResponse revealPan(String cardId, RevealPanRequest request);

    // Regenerate CVV for a user's own DEBIT card after OTP verification (returns one-time plaintext)
    RegenerateCvvResponse regenerateCvv(String cardId, RegenerateCvvRequest request);

    // ADMIN
    List<CardApplicationResponse> listApplicationsByStatus(ApplicationStatus status);

    CardApplicationResponse getApplication(String applicationId);

    CardApplicationResponse reviewApplication(String applicationId, ReviewCardApplicationRequest request);
}
