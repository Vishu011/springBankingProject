// banking-admin-dashboard/src/app/shared/models/card.model.ts

// Enums aligned with CreditCardService backend
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

export type CardApplicationStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';

// Response returned by GET /cards/mine (not used in admin pages now, but kept for completeness)
export interface CardResponse {
  cardId: string;
  userId: string;
  accountId: string;
  type: CardKind;
  brand: CardBrand;
  maskedPan: string; // PAN is always masked in responses
  issueMonth: number; // 1-12
  issueYear: number;  // YYYY
  expiryMonth: number; // 1-12
  expiryYear: number;  // YYYY
  creditLimit?: number | null; // Only for CREDIT cards
  status: CardStatus;
  createdAt?: string;
}

// Response for application endpoints (admin and user)
export interface CardApplicationResponse {
  applicationId: string;
  userId: string;
  accountId: string;
  type: CardKind;
  requestedBrand: CardBrand;
  status: CardApplicationStatus;

  issueMonth?: number | null;
  issueYear?: number | null;
  expiryMonth?: number | null;
  expiryYear?: number | null;

  approvedLimit?: number | null; // Only for CREDIT when approved

  reviewerId?: string | null;
  adminComment?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;

  // Masked values for display (may be present post-approval)
  maskedPan?: string | null;
  maskedCvv?: string | null;

  // Only returned on approval response for admin once
  oneTimeCvv?: string | null;
}

// Request payload for PUT /cards/applications/{id}/review
export interface ReviewCardApplicationRequest {
  decision: 'APPROVED' | 'REJECTED';
  reviewerId: string;
  // Required if decision=APPROVED and type=CREDIT
  approvedLimit?: number | null;
  // Optional override; if omitted backend defaults to now + 5 years
  expiryMonth?: number | null; // 1-12
  expiryYear?: number | null;  // YYYY
  adminComment?: string | null;
}
