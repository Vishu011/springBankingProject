import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../environments/environment";
import { Observable } from "rxjs";

@Injectable({ providedIn: "root" })
export class ApiClientService {
  private ep = environment.endpoints;

  constructor(private http: HttpClient) {}

  // Accounts
  getBalance(accountNumber: string): Observable<number> {
    return this.http.get<number>(`${this.ep.accountMgmtBase}/api/v1/accounts/${accountNumber}/balance`);
  }

  // Payments (internal transfer)
  initiateInternalTransfer(body: any): Observable<any> {
    return this.http.post(`${this.ep.paymentGatewayBase}/api/v1/payments/internal-transfer`, body, {
      headers: { "Idempotency-Key": cryptoRandom() }
    });
  }

  // Beneficiaries
  createBeneficiary(customerId: number, body: any): Observable<any> {
    return this.http.post(`${this.ep.beneficiaryMgmtBase}/api/v1/customers/${customerId}/beneficiaries`, body);
  }

  verifyBeneficiaryOtp(body: any): Observable<any> {
    return this.http.post(`${this.ep.beneficiaryMgmtBase}/api/v1/beneficiaries/verify-otp`, body);
  }

  // Loans
  getLoanSchedule(loanAccountNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.ep.loanMgmtBase}/api/v1/loans/${loanAccountNumber}/schedule`);
  }

  getLoanSummary(loanAccountNumber: string): Observable<any> {
    return this.http.get<any>(`${this.ep.loanMgmtBase}/api/v1/loans/${loanAccountNumber}/summary`);
  }

  // Cards
  submitCardIssuance(body: any): Observable<any> {
    return this.http.post(`${this.ep.cardIssuanceBase}/api/v1/cards/issuance/applications`, body);
  }

  createCardDev(body: any): Observable<any> {
    return this.http.post(`${this.ep.cardMgmtBase}/api/v1/cards/dev/create`, body);
  }

  private cryptoRandom(): string {
    return cryptoRandom();
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
