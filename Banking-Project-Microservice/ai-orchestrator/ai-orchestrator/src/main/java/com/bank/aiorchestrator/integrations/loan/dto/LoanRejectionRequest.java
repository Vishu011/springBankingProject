package com.bank.aiorchestrator.integrations.loan.dto;

/**
 * Mirror of loan-service LoanRejectionRequest with just a reason field.
 */
public class LoanRejectionRequest {
    private String reason;

    public LoanRejectionRequest() {}

    public LoanRejectionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
