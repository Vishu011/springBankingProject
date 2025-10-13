package com.creditcardservice.dto;

import com.creditcardservice.model.CardBrand;
import com.creditcardservice.model.CardKind;
import com.creditcardservice.model.CardStatus;
import lombok.Data;

@Data
public class CardResponse {
    private String cardId;
    private String userId;
    private String accountId;
    private CardKind type;
    private CardBrand brand;
    private String maskedPan; // **** **** **** 1234
    private String maskedCvv; // *** or **** depending on account type
    private Integer issueMonth;
    private Integer issueYear;
    private Integer expiryMonth;
    private Integer expiryYear;
    private Double creditLimit;
    private CardStatus status;
    private String createdAt;
}
