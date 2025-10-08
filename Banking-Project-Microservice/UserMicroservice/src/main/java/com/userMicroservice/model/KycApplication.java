package com.userMicroservice.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a KYC application submitted by a user with identity/address documents.
 */
@Entity
@Table(name = "kyc_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id", updatable = false, nullable = false)
    private String applicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    // Aadhar (12 digits) and PAN (ABCDE1234F)
    @Column(name = "aadhar_number", length = 12, nullable = false)
    private String aadharNumber;

    @Column(name = "pan_number", length = 10, nullable = false)
    private String panNumber;

    // Address captured as part of KYC
    @Column(name = "address_line1", length = 255, nullable = false)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "state", length = 100, nullable = false)
    private String state;

    @Column(name = "postal_code", length = 20, nullable = false)
    private String postalCode;

    // Stored file names for uploaded documents (relative to storage root)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "kyc_application_documents", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "document_path")
    private List<String> documentPaths = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private KycReviewStatus reviewStatus = KycReviewStatus.SUBMITTED;

    @Column(name = "admin_comment", length = 1000)
    private String adminComment;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
