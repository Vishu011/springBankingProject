package com.bank.aiorchestrator.integrations.card.dto;

/**
 * Top-level DTO mirror of CreditCardService ReviewCardApplicationRequest.
 */
public class ReviewCardApplicationRequest {

    public enum Decision { APPROVED, REJECTED }

    private Decision decision;
    private String reviewerId;
    // Required if decision=APPROVED and type=CREDIT
    private Double approvedLimit;
    // Optional override; if null -> default now + 5 years (handled by service)
    private Integer expiryMonth; // 1-12
    private Integer expiryYear;  // YYYY
    private String adminComment;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Double getApprovedLimit() {
        return approvedLimit;
    }

    public void setApprovedLimit(Double approvedLimit) {
        this.approvedLimit = approvedLimit;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }
}
