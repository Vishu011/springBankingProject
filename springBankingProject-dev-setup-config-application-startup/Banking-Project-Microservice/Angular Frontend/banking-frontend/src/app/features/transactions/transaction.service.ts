// src/app/features/transactions/transaction.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  TransactionResponse,
  DepositRequest,
  WithdrawRequest,
  TransferRequest, // Ensure this is the updated interface
  StatementInitiateRequest,
  StatementInitiateResponse,
  StatementVerifyRequest
} from '../../shared/models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {

  private transactionsApiUrl = `${environment.apiUrl}/transactions`; // Base URL for Transaction Microservice

  constructor(private http: HttpClient) { }

  /**
   * Deposits funds into an account.
   * POST /transactions/deposit
   * @param request The deposit request payload.
   * @returns An Observable of the created TransactionResponse.
   */
  depositFunds(request: DepositRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.transactionsApiUrl}/deposit`, request);
  }

  /**
   * Withdraws funds from an account.
   * POST /transactions/withdraw
   * @param request The withdrawal request payload.
   * @returns An Observable of the created TransactionResponse.
   */
  withdrawFunds(request: WithdrawRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.transactionsApiUrl}/withdraw`, request);
  }

  /**
   * Transfers funds between accounts.
   * POST /transactions/transfer
   * @param request The transfer request payload.
   * @returns An Observable of the created TransactionResponse.
   */
  transferFunds(request: TransferRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.transactionsApiUrl}/transfer`, request);
  }

  /**
   * Withdraws funds using a DEBIT card (CVV + OTP required).
   * POST /transactions/debit-card/withdraw
   */
  debitCardWithdraw(request: import('../../shared/models/transaction.model').DebitCardWithdrawRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.transactionsApiUrl}/debit-card/withdraw`, request);
  }

  /**
   * Retrieves transaction history for a specific account.
   * GET /transactions/account/{accountId}
   * @param accountId The ID of the account.
   * @returns An Observable of a list of TransactionResponse.
   */
  getTransactionsByAccountId(accountId: string): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.transactionsApiUrl}/account/${accountId}`);
  }

  /**
   * Retrieves a specific transaction by its ID.
   * GET /transactions/{transactionId}
   * @param transactionId The ID of the transaction.
   * @returns An Observable of the TransactionResponse.
   */
  getTransactionById(transactionId: string): Observable<TransactionResponse> {
    return this.http.get<TransactionResponse>(`${this.transactionsApiUrl}/${transactionId}`);
  }

  /**
   * Initiate account statement OTP to provided email (or fallback to user email).
   * POST /transactions/statements/initiate
   */
  initiateStatement(req: StatementInitiateRequest): Observable<StatementInitiateResponse> {
    return this.http.post<StatementInitiateResponse>(`${this.transactionsApiUrl}/statements/initiate`, req);
  }

  /**
   * Verify OTP, generate password-protected PDF and email to recipient.
   * POST /transactions/statements/verify
   */
  verifyStatement(req: StatementVerifyRequest): Observable<HttpResponse<string>> {
    return this.http.post(`${this.transactionsApiUrl}/statements/verify`, req, {
      observe: 'response',
      responseType: 'text'
    });
  }
}
