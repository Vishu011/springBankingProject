package com.selfservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "self_service_request", indexes = {
        @Index(name = "idx_self_service_req_user", columnList = "user_id"),
        @Index(name = "idx_self_service_req_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id", updatable = false, nullable = false)
    private String requestId;

    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private SelfServiceRequestType type; // NAME_CHANGE, DOB_CHANGE, ADDRESS_CHANGE

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SelfServiceRequestStatus status; // SUBMITTED, APPROVED, REJECTED

    // JSON payload capturing requested changes, e.g. {"firstName":"X","lastName":"Y"} or {"dateOfBirth":"2003-01-01"} or {"address":"..."}
    @Column(name = "payload_json", columnDefinition = "CLOB")
    private String payloadJson;

    // Stored relative paths under uploads/self-service/{userId}/{requestId}/
    @ElementCollection
    private List<String> documents;

    @Column(name = "admin_comment", length = 1024)
    private String adminComment;

    @Column(name = "reviewer_id", length = 64)
    private String reviewerId;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public static SelfServiceRequest submitNew(String userId, SelfServiceRequestType type, String payloadJson, List<String> documents) {
        LocalDateTime now = LocalDateTime.now();
        return SelfServiceRequest.builder()
                .userId(userId)
                .type(type)
                .status(SelfServiceRequestStatus.SUBMITTED)
                .payloadJson(payloadJson)
                .documents(documents)
                .submittedAt(now)
                .build();
    }

    public void approve(String reviewerId, String adminComment) {
        this.status = SelfServiceRequestStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.adminComment = adminComment;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(String reviewerId, String adminComment) {
        this.status = SelfServiceRequestStatus.REJECTED;
        this.reviewerId = reviewerId;
        this.adminComment = adminComment;
        this.reviewedAt = LocalDateTime.now();
    }
}
