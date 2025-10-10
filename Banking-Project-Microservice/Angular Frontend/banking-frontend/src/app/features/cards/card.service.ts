// src/app/features/cards/card.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CardResponse,
  CardApplicationResponse,
  CreateCardApplicationRequestDto
} from '../../shared/models/card.model';

@Injectable({
  providedIn: 'root'
})
export class CardService {

  private cardsApiUrl = `${environment.apiUrl}/cards`; // Base URL for CreditCardService behind API Gateway

  constructor(private http: HttpClient) { }

  /**
   * Submit a new card application (CREDIT or DEBIT).
   * POST /cards/applications
   */
  submitApplication(request: CreateCardApplicationRequestDto): Observable<CardApplicationResponse> {
    return this.http.post<CardApplicationResponse>(`${this.cardsApiUrl}/applications`, request);
  }

  /**
   * List my card applications (for the logged-in user).
   * GET /cards/applications/mine?userId={id}
   */
  listMyApplications(userId: string): Observable<CardApplicationResponse[]> {
    return this.http.get<CardApplicationResponse[]>(
      `${this.cardsApiUrl}/applications/mine`,
      { params: { userId } }
    );
  }

  /**
   * List my issued cards (masked PAN, month/year, brand, status).
   * GET /cards/mine?userId={id}
   */
  listMyCards(userId: string): Observable<CardResponse[]> {
    return this.http.get<CardResponse[]>(
      `${this.cardsApiUrl}/mine`,
      { params: { userId } }
    );
  }

  /**
   * Reveal full PAN for a user's own DEBIT card after OTP verification.
   * POST /cards/{id}/reveal-pan
   */
  revealPan(cardId: string, body: { userId: string; otpCode: string }): Observable<{ cardId: string; fullPan: string; message: string }> {
    return this.http.post<{ cardId: string; fullPan: string; message: string }>(
      `${this.cardsApiUrl}/${cardId}/reveal-pan`,
      body
    );
  }

  /**
   * Regenerate CVV for a user's own DEBIT card after OTP verification.
   * Returns one-time plaintext CVV in the response. Client must not persist it.
   * POST /cards/{id}/regenerate-cvv
   */
  regenerateCvv(cardId: string, body: { userId: string; otpCode: string }): Observable<{ cardId: string; cvv: string; message: string }> {
    return this.http.post<{ cardId: string; cvv: string; message: string }>(
      `${this.cardsApiUrl}/${cardId}/regenerate-cvv`,
      body
    );
  }
}
