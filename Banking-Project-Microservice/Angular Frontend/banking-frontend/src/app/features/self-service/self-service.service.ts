// src/app/features/self-service/self-service.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  NomineeResponse,
  NomineeCreateRequest,
  NomineeUpdateRequest,
  EmailChangeInitiateRequest,
  EmailChangeVerifyRequest,
  PhoneChangeInitiateRequest,
  PhoneChangeVerifyRequest,
  SelfServiceRequestDto,
  SelfServiceRequestType
} from '../../shared/models/self-service.model';

@Injectable({ providedIn: 'root' })
export class SelfServiceService {
  private baseUrl = `${environment.apiUrl}/self-service`;

  constructor(private http: HttpClient) {}

  // ===== Nominees CRUD =====

  listNominees(userId: string): Observable<NomineeResponse[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<NomineeResponse[]>(`${this.baseUrl}/nominees`, { params });
    // GET /self-service/nominees?userId=
  }

  createNominee(req: NomineeCreateRequest): Observable<NomineeResponse> {
    return this.http.post<NomineeResponse>(`${this.baseUrl}/nominees`, req);
    // POST /self-service/nominees
  }

  updateNominee(id: string, req: NomineeUpdateRequest): Observable<NomineeResponse> {
    return this.http.put<NomineeResponse>(`${this.baseUrl}/nominees/${id}`, req);
    // PUT /self-service/nominees/{id}
  }

  deleteNominee(id: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.baseUrl}/nominees/${id}`, { params });
    // DELETE /self-service/nominees/{id}?userId=
  }

  // ===== Contact Updates with OTP =====

  initiateEmailChange(req: EmailChangeInitiateRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/contact/email/initiate`, req);
    // POST /self-service/contact/email/initiate
  }

  verifyEmailChange(req: EmailChangeVerifyRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/contact/email/verify`, req);
    // POST /self-service/contact/email/verify
  }

  initiatePhoneChange(req: PhoneChangeInitiateRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/contact/phone/initiate`, req);
    // POST /self-service/contact/phone/initiate
  }

  verifyPhoneChange(req: PhoneChangeVerifyRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/contact/phone/verify`, req);
    // POST /self-service/contact/phone/verify
  }

  // ===== Self Service Requests (Name/DOB/Address) with documents (multipart) =====

  submitRequestMultipart(args: {
    userId: string;
    type: SelfServiceRequestType;
    payloadJson?: string | null;
    documents: File[];
  }): Observable<SelfServiceRequestDto> {
    const fd = new FormData();
    fd.append('userId', args.userId);
    fd.append('type', args.type);
    fd.append('payloadJson', args.payloadJson ?? '');
    for (const f of args.documents || []) {
      if (f) fd.append('documents', f, f.name);
    }
    return this.http.post<SelfServiceRequestDto>(`${this.baseUrl}/requests`, fd);
    // POST /self-service/requests (multipart)
  }

  listMyRequests(userId: string): Observable<SelfServiceRequestDto[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<SelfServiceRequestDto[]>(`${this.baseUrl}/requests/mine`, { params });
    // GET /self-service/requests/mine?userId=
  }
}
