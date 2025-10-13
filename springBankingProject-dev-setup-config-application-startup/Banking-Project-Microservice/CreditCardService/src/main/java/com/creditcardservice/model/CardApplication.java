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
public class CardApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String applicationId;

    private String userId;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private CardKind type; // CREDIT or DEBIT

    @Enumerated(EnumType.STRING)
    private CardBrand requestedBrand; // VISA, RUPAY, AMEX, MASTERCARD, DISCOVERY

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private String reviewerId;

    @Column(length = 1024)
    private String adminComment;

    // Approval fields
    private Double approvedLimit; // nullable for DEBIT

    private Integer issueMonth; // 1-12
    private Integer issueYear;  // YYYY

    private Integer expiryMonth; // 1-12
    private Integer expiryYear;  // YYYY

    // Generated upon approval
    private String generatedCardNumber; // PAN (masked in responses elsewhere)
    private String generatedCvvMasked;  // store masked for audit display-only; actual CVV will be hashed in Card entity

    // For optimistic checks or audits
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.applicationId == null) {
            this.applicationId = UUID.randomUUID().toString();
        }
        this.submittedAt = this.submittedAt == null ? LocalDateTime.now() : this.submittedAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = ApplicationStatus.SUBMITTED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ApplicationStatus {
        SUBMITTED,
        APPROVED,
        REJECTED
    }
}
