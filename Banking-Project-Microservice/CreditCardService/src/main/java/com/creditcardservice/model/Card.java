package com.creditcardservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String cardId;

    private String userId;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private CardKind type; // CREDIT or DEBIT

    @Enumerated(EnumType.STRING)
    private CardBrand brand; // VISA, RUPAY, AMEX, MASTERCARD, DISCOVERY

    @Column(unique = true, nullable = false)
    private String cardNumber; // PAN (encrypt/tokenize in future; ensure masking in responses)

    // Store CVV as a secure hash (never return plaintext)
    private String cvvHash;

    // Month/Year only as per requirements
    private Integer issueMonth; // 1-12
    private Integer issueYear;  // YYYY
    private Integer expiryMonth; // 1-12
    private Integer expiryYear;  // YYYY

    // For CREDIT cards only; null for DEBIT
    private Double creditLimit;

    @Enumerated(EnumType.STRING)
    private CardStatus status; // ACTIVE, BLOCKED

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (this.cardId == null) {
            this.cardId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CardStatus.ACTIVE;
        }
    }
}
