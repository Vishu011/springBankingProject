// src/app/shared/models/salary-application.model.ts

export enum SalaryApplicationStatus {
  SUBMITTED = 'SUBMITTED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export interface CreateSalaryApplicationRequest {
  userId: string;
  corporateEmail: string;
  otpCode: string;
  documents?: string[];
}

export interface SalaryApplicationResponse {
  applicationId: string;
  userId: string;
  corporateEmail: string;
  documents: string[];
  status: SalaryApplicationStatus;
  adminComment?: string | null;
  submittedAt: string;   // ISO date string
  reviewedAt?: string | null;  // ISO date string
  reviewerId?: string | null;
}
