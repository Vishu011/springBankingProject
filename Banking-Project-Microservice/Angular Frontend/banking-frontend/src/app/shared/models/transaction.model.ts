// src/app/shared/models/transaction.model.ts

// Enums mirroring backend Transaction.java
export enum TransactionType {
    DEPOSIT = 'DEPOSIT',
    WITHDRAW = 'WITHDRAW',
    TRANSFER = 'TRANSFER',
}

export enum TransactionStatus {
    SUCCESS = 'SUCCESS',
    FAILED = 'FAILED',
    PENDING = 'PENDING',
}

// Interface mirroring backend Transaction entity/response DTO
export interface TransactionResponse {
    transactionId: string;
    fromAccountId: string | null; // Can be null for DEPOSIT
    toAccountId: string | null;   // Can be null for WITHDRAW
    amount: number;
    type: TransactionType;
    status: TransactionStatus;
    transactionDate: string; // Use string for LocalDateTime from Java
    metadataJson?: string; // Optional JSON string with extra info (e.g., { method, brand, panMasked })
}

// Interface mirroring backend DepositRequest DTO
export interface DepositRequest {
    accountId: string;
    amount: number;
}

// Interface mirroring backend WithdrawRequest DTO
export interface WithdrawRequest {
    accountId: string;
    amount: number;
    otpCode: string; // OTP from user, required by backend
}

// Interface mirroring backend TransferRequest DTO
export interface TransferRequest {
    fromAccountNumber: string; // Changed from fromAccountId
    toAccountNumber: string;   // Changed from toAccountId
    amount: number;
    otpCode: string; // OTP from user, required by backend for transfer
}

// Request to perform withdrawal using a debit card
export interface DebitCardWithdrawRequest {
    cardNumber: string;
    cvv: string;
    amount: number;
    otpCode: string;
}

/**
 * Account statement OTP + generation DTOs
 */
export interface StatementInitiateRequest {
    userId: string;
    accountId: string;
    fromDate: string; // yyyy-MM-dd
    toDate: string;   // yyyy-MM-dd
    toEmail?: string; // optional override recipient
}

export interface StatementInitiateResponse {
    requestId?: string | null;
    expiresAt?: string | null; // ISO date-time
}

export interface StatementVerifyRequest {
    userId: string;
    accountId: string;
    fromDate: string; // yyyy-MM-dd
    toDate: string;   // yyyy-MM-dd
    code: string;     // OTP code
    toEmail?: string; // optional override recipient
}
