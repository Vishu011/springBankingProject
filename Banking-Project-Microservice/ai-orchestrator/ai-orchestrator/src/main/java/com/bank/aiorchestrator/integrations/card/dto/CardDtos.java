package com.bank.aiorchestrator.integrations.card.dto;

/**
 * DTO mirrors for CreditCardService to decouple orchestrator from direct enums/classes.
 */
public class CardDtos {

    /**
     * Mirror of CreditCardService CardApplicationResponse with looser typing.
     */
    public static class CardApplicationResponse {
        private String oneTimeCvv;
        private String applicationId;
        private String userId;
        private String accountId;
        private String type;                // DEBIT or CREDIT
        private String requestedBrand;      // BRAND enum as String
        private String status;              // SUBMITTED/APPROVED/REJECTED

        private Integer issueMonth;
        private Integer issueYear;
        private Integer expiryMonth;
        private Integer expiryYear;

        private Double approvedLimit;       // credit only

        private String maskedPan;
        private String maskedCvv;

        private String reviewerId;
        private String adminComment;

        private String submittedAt;         // keep as String to avoid parse mismatch
        private String reviewedAt;

        public String getOneTimeCvv() { return oneTimeCvv; }
        public void setOneTimeCvv(String oneTimeCvv) { this.oneTimeCvv = oneTimeCvv; }
        public String getApplicationId() { return applicationId; }
        public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRequestedBrand() { return requestedBrand; }
        public void setRequestedBrand(String requestedBrand) { this.requestedBrand = requestedBrand; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getIssueMonth() { return issueMonth; }
        public void setIssueMonth(Integer issueMonth) { this.issueMonth = issueMonth; }
        public Integer getIssueYear() { return issueYear; }
        public void setIssueYear(Integer issueYear) { this.issueYear = issueYear; }
        public Integer getExpiryMonth() { return expiryMonth; }
        public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }
        public Integer getExpiryYear() { return expiryYear; }
        public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }
        public Double getApprovedLimit() { return approvedLimit; }
        public void setApprovedLimit(Double approvedLimit) { this.approvedLimit = approvedLimit; }
        public String getMaskedPan() { return maskedPan; }
        public void setMaskedPan(String maskedPan) { this.maskedPan = maskedPan; }
        public String getMaskedCvv() { return maskedCvv; }
        public void setMaskedCvv(String maskedCvv) { this.maskedCvv = maskedCvv; }
        public String getReviewerId() { return reviewerId; }
        public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
        public String getAdminComment() { return adminComment; }
        public void setAdminComment(String adminComment) { this.adminComment = adminComment; }
        public String getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
        public String getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }
    }

    /**
     * Mirror of CreditCardService ReviewCardApplicationRequest.
     */
    public static class ReviewCardApplicationRequest {

        public enum Decision { APPROVED, REJECTED }

        private Decision decision;
        private String reviewerId;
        private Double approvedLimit;   // for CREDIT if APPROVED
        private Integer expiryMonth;    // optional override
        private Integer expiryYear;     // optional override
        private String adminComment;

        public Decision getDecision() { return decision; }
        public void setDecision(Decision decision) { this.decision = decision; }
        public String getReviewerId() { return reviewerId; }
        public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
        public Double getApprovedLimit() { return approvedLimit; }
        public void setApprovedLimit(Double approvedLimit) { this.approvedLimit = approvedLimit; }
        public Integer getExpiryMonth() { return expiryMonth; }
        public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }
        public Integer getExpiryYear() { return expiryYear; }
        public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }
        public String getAdminComment() { return adminComment; }
        public void setAdminComment(String adminComment) { this.adminComment = adminComment; }
    }
}
