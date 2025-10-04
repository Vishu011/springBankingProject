import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../environments/environment";
import { Observable } from "rxjs";

@Injectable({ providedIn: "root" })
export class ApiClientService {
  private ep = environment.endpoints;

  constructor(private http: HttpClient) {}

  // ========== Accounts ==========
  getBalance(accountNumber: string): Observable<number> {
    return this.http.get<number>(`${this.ep.accountMgmtBase}/api/v1/accounts/${accountNumber}/balance`);
  }

  // ========== Payments (internal transfer) ==========
  initiateInternalTransfer(body: {
    customerId: number;
    fromAccount: string;
    toAccount: string;
    amount: number;
    currency: string;
  }): Observable<{ paymentId: string; status: string }> {
    return this.http.post<{ paymentId: string; status: string }>(
      `${this.ep.paymentGatewayBase}/api/v1/payments/internal-transfer`,
      body,
      { headers: { "Idempotency-Key": cryptoRandom() } }
    );
  }

  getPaymentStatus(paymentId: string): Observable<string> {
    return this.http.get(`${this.ep.paymentGatewayBase}/api/v1/payments/${paymentId}/status`, {
      responseType: "text",
    });
  }

  // ========== Beneficiaries ==========
  createBeneficiary(customerId: number, body: { nickname: string; accountNumber: string; bankCode: string }): Observable<any> {
    return this.http.post(`${this.ep.beneficiaryMgmtBase}/api/v1/customers/${customerId}/beneficiaries`, body);
  }

  verifyBeneficiaryOtp(body: {
    owningCustomerId: number;
    beneficiaryId: number;
    challengeId: number;
    code: string;
  }): Observable<any> {
    return this.http.post(`${this.ep.beneficiaryMgmtBase}/api/v1/beneficiaries/verify-otp`, body);
  }

  listBeneficiaries(customerId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.ep.beneficiaryMgmtBase}/api/v1/customers/${customerId}/beneficiaries`);
  }

  // ========== Loans ==========
  getLoanSchedule(loanAccountNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.ep.loanMgmtBase}/api/v1/loans/${loanAccountNumber}/schedule`);
  }

  getLoanSummary(loanAccountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.ep.loanMgmtBase}/api/v1/loans/${loanAccountNumber}/summary`);
  }

  applyEmiDev(loanAccountNumber: string, body: { transactionId: string; amount: number }): Observable<void> {
    return this.http.post<void>(`${this.ep.loanMgmtBase}/api/v1/loans/internal/dev/loans/${loanAccountNumber}/apply-emi`, body);
  }

  // ========== Cards: Issuance ==========
  submitCardIssuance(body: { customerId: number; productType?: string }): Observable<{ applicationId: string; status: string }> {
    return this.http.post<{ applicationId: string; status: string }>(
      `${this.ep.cardIssuanceBase}/api/v1/cards/issuance/applications`,
      body
    );
  }

  getIssuanceApplication(appId: string): Observable<any> {
    return this.http.get<any>(`${this.ep.cardIssuanceBase}/api/v1/cards/issuance/applications/${appId}`);
  }

  runEligibility(appId: string): Observable<any> {
    return this.http.post<any>(`${this.ep.cardIssuanceBase}/api/v1/cards/issuance/applications/${appId}/eligibility-check`, {});
  }

  approveIssuance(appId: string): Observable<any> {
    return this.http.post<any>(`${this.ep.cardIssuanceBase}/api/v1/cards/issuance/applications/${appId}/approve`, {});
  }

  // ========== Cards: Management ==========
  createCardDev(body: { customerId: number; productType?: string; initialLimit?: number }): Observable<{ cardId: string; status: string }> {
    return this.http.post<{ cardId: string; status: string }>(`${this.ep.cardMgmtBase}/api/v1/cards/dev/create`, body);
  }

  getCard(cardId: string): Observable<any> {
    return this.http.get<any>(`${this.ep.cardMgmtBase}/api/v1/cards/${cardId}`);
  }

  activateCard(cardId: string): Observable<any> {
    return this.http.post<any>(`${this.ep.cardMgmtBase}/api/v1/cards/${cardId}/activate`, {});
  }

  setCardLimits(cardId: string, limit: number): Observable<any> {
    return this.http.post<any>(`${this.ep.cardMgmtBase}/api/v1/cards/${cardId}/limits`, { limit });
  }

  updateCardStatus(cardId: string, status: "BLOCK" | "UNBLOCK"): Observable<any> {
    return this.http.post<any>(`${this.ep.cardMgmtBase}/api/v1/cards/${cardId}/status`, { status });
  }

  getCardByIssuanceApp(appId: string): Observable<any> {
    return this.http.get<any>(`${this.ep.cardMgmtBase}/api/v1/cards/issuance/${appId}`);
  }

  listCardsByCustomer(customerId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.ep.cardMgmtBase}/api/v1/cards/customers/${customerId}`);
  }
}

function cryptoRandom(): string {
  try {
    // @ts-ignore
    if (typeof crypto !== "undefined" && crypto.randomUUID) {
      // @ts-ignore
      return crypto.randomUUID();
    }
  } catch {}
  return "idem-" + Math.random().toString(36).slice(2);
}
