// src/app/shared/models/self-service.model.ts

export enum NomineeRelationship {
  MOTHER = 'MOTHER',
  FATHER = 'FATHER',
  SIBLING = 'SIBLING',
  WIFE = 'WIFE',
  HUSBAND = 'HUSBAND',
  GUARDIAN = 'GUARDIAN',
  OTHER = 'OTHER'
}

export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER'
}

export interface NomineeResponse {
  id: string;
  userId: string;
  name: string;
  age: number;
  gender: Gender;
  relationship: NomineeRelationship;
  percentageShare?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface NomineeCreateRequest {
  userId: string;
  name: string;
  age: number;
  gender: Gender;
  relationship: NomineeRelationship;
  percentageShare?: number | null;
}

export interface NomineeUpdateRequest {
  name?: string;
  age?: number;
  gender?: Gender;
  relationship?: NomineeRelationship;
  percentageShare?: number | null;
}

export enum SelfServiceRequestType {
  NAME_CHANGE = 'NAME_CHANGE',
  DOB_CHANGE = 'DOB_CHANGE',
  ADDRESS_CHANGE = 'ADDRESS_CHANGE'
}

export enum SelfServiceRequestStatus {
  SUBMITTED = 'SUBMITTED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export interface SelfServiceRequestDto {
  id: string;
  userId: string;
  type: SelfServiceRequestType;
  payloadJson: string | null;
  documents: string[] | null;
  status: SelfServiceRequestStatus;
  adminComment?: string | null;
  reviewerId?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;
}

// Contact change DTOs (frontend payloads)
export interface EmailChangeInitiateRequest {
  userId: string;
  newEmail: string;
}

export interface EmailChangeVerifyRequest {
  userId: string;
  newEmail: string;
  code: string;
}

export interface PhoneChangeInitiateRequest {
  userId: string;
  newPhone: string;
}

export interface PhoneChangeVerifyRequest {
  userId: string;
  newPhone: string;
  code: string;
}
