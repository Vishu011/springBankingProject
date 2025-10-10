package com.accountMicroservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Salary/Corporate Account Application entity.
 * Stores corporate email verification and uploaded document references (as JSON) for review.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryAccountApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id", updatable = false, nullable = false)
    private String applicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "corporate_email", nullable = false)
    private String corporateEmail;

    /**
     * JSON array string of document locations/paths (e.g., ["s3://...","/files/..."])
     * Using CLOB to avoid size constraints.
     */
    @Lob
    @Column(name = "documents_json", nullable = true)
    private String documentsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalaryApplicationStatus status = SalaryApplicationStatus.SUBMITTED;

    @Column(name = "admin_comment")
    private String adminComment;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_id")
    private String reviewerId;
}
