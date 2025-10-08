import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type KycReviewStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface KycApplication {
  applicationId: string;
  userId: string;
  aadharNumber: string;
  panNumber: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  state: string;
  postalCode: string;
  documentPaths: string[];
  reviewStatus: KycReviewStatus;
  adminComment?: string | null;
  submittedAt: string;
  reviewedAt?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AdminKycService {
  private baseUrl = `${environment.apiUrl}/auth/kyc`;

  constructor(private http: HttpClient) {}

  listApplicationsByStatus(status: KycReviewStatus = 'SUBMITTED'): Observable<KycApplication[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<KycApplication[]>(`${this.baseUrl}/applications`, { params });
  }

  getApplicationById(applicationId: string): Observable<KycApplication> {
    return this.http.get<KycApplication>(`${this.baseUrl}/applications/${applicationId}`);
  }

  reviewApplication(applicationId: string, decision: 'APPROVED' | 'REJECTED', adminComment?: string): Observable<KycApplication> {
    let params = new HttpParams().set('decision', decision);
    if (adminComment) params = params.set('adminComment', adminComment);
    return this.http.put<KycApplication>(`${this.baseUrl}/applications/${applicationId}/review`, null, { params });
  }

  /**
   * Download a stored KYC document as a Blob and return it for the caller to handle saving.
   * The backend endpoint expects `path` query param which is the relative storage path.
   */
  downloadDocument(applicationId: string, relPath: string) {
    const params = new HttpParams().set('path', relPath);
    return this.http.get(`${this.baseUrl}/applications/${applicationId}/documents`, {
      params,
      responseType: 'blob'
    });
  }
}
