// src/app/features/self-service-requests/self-service-requests.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminSelfServiceRequest {
  requestId: string;         // assuming entity uses 'requestId' as id
  userId: string;
  type: 'NAME_CHANGE' | 'DOB_CHANGE' | 'ADDRESS_CHANGE';
  status: 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'PENDING' | 'UNKNOWN';
  adminComment?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;
  payloadJson?: string | null;
  documents?: string[] | { fileName: string; relativePath: string; size?: number }[]; // tolerant shape
}

export interface AdminDecisionBody {
  adminComment?: string | null;
  reviewerId?: string | null; // optional; if omitted backend will default
}

@Injectable({ providedIn: 'root' })
export class SelfServiceRequestsAdminService {
  private baseUrl = `${environment.apiUrl}/self-service/admin/requests`;

  constructor(private http: HttpClient) {}

  list(status: 'SUBMITTED' | 'APPROVED' | 'REJECTED' | string = 'SUBMITTED'): Observable<AdminSelfServiceRequest[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<AdminSelfServiceRequest[]>(`${this.baseUrl}`, { params });
  }

  getOne(requestId: string): Observable<AdminSelfServiceRequest> {
    return this.http.get<AdminSelfServiceRequest>(`${this.baseUrl}/${encodeURIComponent(requestId)}`);
  }

  approve(requestId: string, body: AdminDecisionBody): Observable<AdminSelfServiceRequest> {
    return this.http.post<AdminSelfServiceRequest>(`${this.baseUrl}/${encodeURIComponent(requestId)}/approve`, body || {});
  }

  reject(requestId: string, body: AdminDecisionBody): Observable<AdminSelfServiceRequest> {
    return this.http.post<AdminSelfServiceRequest>(`${this.baseUrl}/${encodeURIComponent(requestId)}/reject`, body || {});
  }

  downloadDocument(requestId: string, relativePath: string): Observable<Blob> {
    // The backend download endpoint expects everything after '/documents/' as relative path
    return this.http.get(`${this.baseUrl}/${encodeURIComponent(requestId)}/documents/${relativePath}`, {
      responseType: 'blob'
    });
  }
}
