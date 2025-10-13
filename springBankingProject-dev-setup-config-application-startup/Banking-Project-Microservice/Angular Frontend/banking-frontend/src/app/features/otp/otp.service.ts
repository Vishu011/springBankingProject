// src/app/features/otp/otp.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface GenerateOtpRequest {
  userId: string;
  purpose: 'LOGIN' | 'WITHDRAWAL' | 'LOAN_SUBMISSION' | 'CARD_OPERATION' | 'ACCOUNT_OPERATION' | 'CONTACT_VERIFICATION' | 'CARD_ISSUANCE';
  channels?: string[]; // default EMAIL
  contextId?: string | null;
  ttlSeconds?: number;
}

export interface GenerateOtpResponse {
  requestId: string;
  expiresAt: string;
}

export interface VerifyOtpRequest {
  userId: string;
  purpose: 'LOGIN' | 'WITHDRAWAL' | 'LOAN_SUBMISSION' | 'CARD_OPERATION' | 'ACCOUNT_OPERATION' | 'CONTACT_VERIFICATION' | 'CARD_ISSUANCE';
  contextId?: string | null;
  code: string;
}

export interface VerifyOtpResponse {
  verified: boolean;
  requestId: string;
  verifiedAt?: string;
  remainingAttempts?: number;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class OtpService {
  private baseUrl = `${environment.apiUrl}/otp`;

  constructor(private http: HttpClient) {}

  generate(req: GenerateOtpRequest): Observable<GenerateOtpResponse> {
    return this.http.post<GenerateOtpResponse>(`${this.baseUrl}/generate`, req);
  }

  verify(req: VerifyOtpRequest): Observable<VerifyOtpResponse> {
    return this.http.post<VerifyOtpResponse>(`${this.baseUrl}/verify`, req);
  }

  // Public endpoints for unauthenticated flows (e.g., registration CONTACT_VERIFICATION)
  generatePublic(req: GenerateOtpRequest): Observable<GenerateOtpResponse> {
    return this.http.post<GenerateOtpResponse>(`${this.baseUrl}/public/generate`, req);
  }

  verifyPublic(req: VerifyOtpRequest): Observable<VerifyOtpResponse> {
    return this.http.post<VerifyOtpResponse>(`${this.baseUrl}/public/verify`, req);
  }
}
