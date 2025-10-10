// banking-admin-dashboard/src/app/features/salary-applications/salary-application.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type SalaryApplicationStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface SalaryApplication {
  applicationId: string;
  userId: string;
  corporateEmail: string;
  documents: string[];
  status: SalaryApplicationStatus;
  adminComment?: string | null;
  submittedAt: string;  // ISO strings from backend
  reviewedAt?: string | null;
  reviewerId?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AdminSalaryApplicationService {
  private baseUrl = `${environment.apiUrl}/accounts/salary/applications`;

  constructor(private http: HttpClient) {}

  listByStatus(status: SalaryApplicationStatus = 'SUBMITTED'): Observable<SalaryApplication[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<SalaryApplication[]>(this.baseUrl, { params });
  }

  getOne(id: string): Observable<SalaryApplication> {
    return this.http.get<SalaryApplication>(`${this.baseUrl}/${id}`);
  }

  review(id: string, decision: SalaryApplicationStatus, reviewerId?: string, adminComment?: string): Observable<SalaryApplication> {
    let params = new HttpParams().set('decision', decision);
    if (adminComment) params = params.set('adminComment', adminComment);
    if (reviewerId) params = params.set('reviewerId', reviewerId);
    return this.http.put<SalaryApplication>(`${this.baseUrl}/${id}/review`, null, { params });
  }

  downloadDocument(applicationId: string, path: string): Observable<Blob> {
    const params = new HttpParams().set('path', path);
    return this.http.get(`${this.baseUrl}/${applicationId}/documents`, {
      params,
      responseType: 'blob'
    });
  }
}
