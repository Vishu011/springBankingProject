// src/app/features/accounts/salary-application/salary-application.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Observable } from 'rxjs';
import {
  CreateSalaryApplicationRequest,
  SalaryApplicationResponse
} from '../../../shared/models/salary-application.model';

@Injectable({
  providedIn: 'root'
})
export class SalaryApplicationService {
  private baseUrl = `${environment.apiUrl}/accounts/salary/applications`;

  constructor(private http: HttpClient) {}

  submitApplication(req: CreateSalaryApplicationRequest): Observable<SalaryApplicationResponse> {
    return this.http.post<SalaryApplicationResponse>(this.baseUrl, req);
  }

  getMyApplications(userId: string): Observable<SalaryApplicationResponse[]> {
    return this.http.get<SalaryApplicationResponse[]>(`${this.baseUrl}/mine`, {
      params: { userId }
    });
  }

  getApplication(id: string): Observable<SalaryApplicationResponse> {
    return this.http.get<SalaryApplicationResponse>(`${this.baseUrl}/${id}`);
  }

  submitApplicationMultipart(userId: string, corporateEmail: string, otpCode: string, files: File[]): Observable<SalaryApplicationResponse> {
    const formData = new FormData();
    formData.append('userId', userId);
    formData.append('corporateEmail', corporateEmail);
    formData.append('otpCode', otpCode);
    (files || []).forEach(f => formData.append('documents', f));
    return this.http.post<SalaryApplicationResponse>(this.baseUrl, formData);
  }
}
