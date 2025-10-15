package com.bank.aiorchestrator.integrations.selfservice.dto;

/**
 * Mirror of AdminDecisionRequest for SelfService admin endpoints.
 */
public class SelfServiceAdminDecisionRequest {
    private String adminComment;
    private String reviewerId;

    public SelfServiceAdminDecisionRequest() {}

    public SelfServiceAdminDecisionRequest(String adminComment, String reviewerId) {
        this.adminComment = adminComment;
        this.reviewerId = reviewerId;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }
}
