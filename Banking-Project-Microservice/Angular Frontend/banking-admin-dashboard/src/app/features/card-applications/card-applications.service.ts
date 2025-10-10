// banking-admin-dashboard/src/app/features/card-applications/card-applications.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CardApplicationResponse,
  ReviewCardApplicationRequest
} from '../../shared/models/card.model';

export type CardApplicationStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';

@Injectable({
  providedIn: 'root'
})
export class CardApplicationsAdminService {
  private baseUrl = `${environment.apiUrl}/cards/applications`;

  constructor(private http: HttpClient) {}

  listByStatus(status: CardApplicationStatus = 'SUBMITTED'): Observable<CardApplicationResponse[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<CardApplicationResponse[]>(this.baseUrl, { params });
  }

  getOne(id: string): Observable<CardApplicationResponse> {
    return this.http.get<CardApplicationResponse>(`${this.baseUrl}/${id}`);
  }

  review(id: string, payload: ReviewCardApplicationRequest): Observable<CardApplicationResponse> {
    return this.http.put<CardApplicationResponse>(`${this.baseUrl}/${id}/review`, payload);
  }
}
