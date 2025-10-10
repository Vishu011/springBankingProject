// src/app/shared/models/card.model.ts

// Aligning with new backend models in CreditCardService

export enum CardBrand {
  VISA = 'VISA',
  RUPAY = 'RUPAY',
  AMEX = 'AMEX',
  MASTERCARD = 'MASTERCARD',
  DISCOVERY = 'DISCOVERY'
}

export enum CardKind {
  CREDIT = 'CREDIT',
  DEBIT = 'DEBIT'
}

export enum CardStatus {
  ACTIVE = 'ACTIVE',
  BLOCKED = 'BLOCKED'
}

// Response returned by GET /cards/mine
export interface CardResponse {
  cardId: string;
  userId: string;
  accountId: string;
  type: CardKind;
  brand: CardBrand;
  maskedPan: string; // PAN is always masked in responses
  maskedCvv?: string | null; // server-provided masked CVV length (*** or ****)
  issueMonth: number; // 1-12
  issueYear: number;  // YYYY
  expiryMonth: number; // 1-12
  expiryYear: number;  // YYYY
  creditLimit?: number | null; // Only for CREDIT cards
  status: CardStatus;
  createdAt?: string;
}

// DTO for creating an application: POST /cards/applications
export interface CreateCardApplicationRequestDto {
  userId: string;
  accountId: string;
  type: CardKind; // CREDIT or DEBIT
  requestedBrand: CardBrand;
  otpCode: string;
}

// Response returned by application endpoints
export interface CardApplicationResponse {
  applicationId: string;
  userId: string;
  accountId: string;
  type: CardKind;
  requestedBrand: CardBrand;
  status: 'SUBMITTED' | 'APPROVED' | 'REJECTED';

  issueMonth?: number | null;
  issueYear?: number | null;
  expiryMonth?: number | null;
  expiryYear?: number | null;

  approvedLimit?: number | null; // Only for CREDIT when approved

  reviewerId?: string | null;
  adminComment?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;

  // Masked values for display
  maskedPan?: string | null;
  maskedCvv?: string | null;

  // Only returned on approval response for admin once; not applicable on user side
  oneTimeCvv?: string | null;
}
