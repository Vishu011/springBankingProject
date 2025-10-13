import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type KycReviewStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface KycApplication {
  applicationId: string;
  userId: string;
  aadharNumber: string;
  panNumber: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  documentPaths: string[];
  reviewStatus: KycReviewStatus;
  adminComment?: string | null;
  submittedAt: string;
  reviewedAt?: string | null;
}

export interface KycSubmitPayload {
  aadharNumber: string;
  panNumber: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  documents?: File[];
}

@Injectable({
  providedIn: 'root'
})
export class KycService {
  private baseUrl = `${environment.apiUrl}/auth/kyc`;

  constructor(private http: HttpClient) {}

  /**
   * Submit KYC application for the authenticated user.
   * Backend derives userId from Authentication principal; no need to send userId.
   */
  submitApplication(payload: KycSubmitPayload): Observable<KycApplication> {
    const formData = new FormData();

    formData.append('aadharNumber', payload.aadharNumber);
    formData.append('panNumber', payload.panNumber);
    formData.append('addressLine1', payload.addressLine1);
    if (payload.addressLine2) formData.append('addressLine2', payload.addressLine2);
    formData.append('city', payload.city);
    formData.append('state', payload.state);
    formData.append('postalCode', payload.postalCode);

    if (payload.documents && payload.documents.length) {
      for (const file of payload.documents) {
        // Spring will bind this to MultipartFile[] documents
        formData.append('documents', file, file.name);
      }
    }

    return this.http.post<KycApplication>(`${this.baseUrl}/applications`, formData);
  }

  /**
   * ADMIN: List applications by status (default SUBMITTED)
   */
  listApplicationsByStatus(status: KycReviewStatus = 'SUBMITTED'): Observable<KycApplication[]> {
    return this.http.get<KycApplication[]>(`${this.baseUrl}/applications`, { params: { status } });
  }

  /**
   * ADMIN: Review application
   */
  reviewApplication(applicationId: string, decision: KycReviewStatus, adminComment?: string): Observable<KycApplication> {
    const params: Record<string, string> = { decision };
    if (adminComment) params['adminComment'] = adminComment;
    return this.http.put<KycApplication>(`${this.baseUrl}/applications/${applicationId}/review`, null, { params });
  }
}
