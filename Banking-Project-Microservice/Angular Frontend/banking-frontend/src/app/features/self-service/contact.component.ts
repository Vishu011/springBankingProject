// src/app/features/self-service/contact.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { SelfServiceService } from './self-service.service';
import { AuthService } from '../../core/services/auth.service';
import {
  EmailChangeInitiateRequest,
  EmailChangeVerifyRequest,
  PhoneChangeInitiateRequest,
  PhoneChangeVerifyRequest
} from '../../shared/models/self-service.model';

@Component({
  selector: 'app-self-service-contact',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="container">
    <h2>Contact Details</h2>
    <div *ngIf="globalError" class="error">{{ globalError }}</div>
    <div *ngIf="globalSuccess" class="success">{{ globalSuccess }}</div>

    <!-- Email Update -->
    <div class="card" id="email-section">
      <h3>Update Email</h3>
      <div class="row">
        <label>New Email</label>
        <input type="email" [(ngModel)]="newEmail" />
        <button type="button" (click)="initiateEmail()" [disabled]="!newEmail || emailInitiating">Send OTP</button>
      </div>
      <div class="row" *ngIf="emailOtpSent">
        <label>Enter OTP</label>
        <input type="text" [(ngModel)]="emailOtp" maxlength="6" placeholder="6-digit code" />
        <button type="button" (click)="verifyEmail()" [disabled]="!emailOtp || emailVerifying">Verify & Update</button>
      </div>
      <small class="help-text">
        Note: OTP will be sent to the new email address to confirm ownership.
      </small>
      <div *ngIf="emailError" class="error">{{ emailError }}</div>
      <div *ngIf="emailSuccess" class="success">{{ emailSuccess }}</div>
    </div>

    <!-- Phone Update -->
    <div class="card" id="phone-section">
      <h3>Update Phone</h3>
      <div class="row">
        <label>New Phone</label>
        <input type="tel" [(ngModel)]="newPhone" placeholder="e.g., 9876543210" />
        <button type="button" (click)="initiatePhone()" [disabled]="!newPhone || phoneInitiating">Send OTP</button>
      </div>
      <div class="row" *ngIf="phoneOtpSent">
        <label>Enter OTP (sent to your registered email)</label>
        <input type="text" [(ngModel)]="phoneOtp" maxlength="6" placeholder="6-digit code" />
        <button type="button" (click)="verifyPhone()" [disabled]="!phoneOtp || phoneVerifying">Verify & Update</button>
      </div>
      <small class="help-text">
        Note: Phone OTP will be sent to your registered email for security.
      </small>
      <div *ngIf="phoneError" class="error">{{ phoneError }}</div>
      <div *ngIf="phoneSuccess" class="success">{{ phoneSuccess }}</div>
    </div>
  </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    .card { border:1px solid #e0e0e0; border-radius:8px; padding:1rem; margin-bottom:1rem; background:#fff; }
    .row { display:flex; gap:.75rem; align-items:center; margin-bottom:.5rem; flex-wrap:wrap; }
    .row label { width: 140px; }
    input { padding:.4rem .5rem; border:1px solid #ccc; border-radius:4px; }
    button { padding:.4rem .8rem; border:none; background:#0d6efd; color:#fff; border-radius:4px; cursor:pointer; }
    button[disabled] { opacity:.6; cursor:not-allowed; }
    .error { color:#b00020; margin-bottom:.5rem; }
    .success { color:#1b5e20; margin-bottom:.5rem; }
    .help-text { color:#777; display:block; }
  `]
})
export class SelfServiceContactComponent implements OnInit {
  userId: string | null = null;

  // Email flow
  newEmail = '';
  emailOtp = '';
  emailOtpSent = false;
  emailInitiating = false;
  emailVerifying = false;
  emailError: string | null = null;
  emailSuccess: string | null = null;

  // Phone flow
  newPhone = '';
  phoneOtp = '';
  phoneOtpSent = false;
  phoneInitiating = false;
  phoneVerifying = false;
  phoneError: string | null = null;
  phoneSuccess: string | null = null;

  // Global
  globalError: string | null = null;
  globalSuccess: string | null = null;

  constructor(
    private selfService: SelfServiceService,
    private auth: AuthService,
    private route: ActivatedRoute
  ) {
    this.userId = this.auth.getIdentityClaims()?.sub || null;
    if (!this.userId) {
      this.globalError = 'User not found. Please log in again.';
    }
  }

  ngOnInit(): void {
    const focus = this.route.snapshot.queryParamMap.get('focus');
    if (focus) { this.scrollToFocus(focus); }
  }

  private scrollToFocus(focus: string): void {
    const id = focus === 'email' ? 'email-section' : focus === 'phone' ? 'phone-section' : null;
    if (id) {
      setTimeout(() => {
        const el = document.getElementById(id);
        if (el) { el.scrollIntoView({ behavior: 'smooth', block: 'start' }); }
      }, 100);
    }
  }

  initiateEmail(): void {
    if (!this.userId || !this.newEmail) return;
    this.resetEmailMessages();
    this.emailInitiating = true;
    const req: EmailChangeInitiateRequest = { userId: this.userId, newEmail: this.newEmail };
    this.selfService.initiateEmailChange(req).subscribe({
      next: () => {
        this.emailOtpSent = true;
        this.emailSuccess = 'OTP sent to the new email address.';
        this.emailInitiating = false;
      },
      error: (err) => {
        this.emailError = err?.error?.message || 'Failed to send OTP to new email.';
        this.emailInitiating = false;
      }
    });
  }

  verifyEmail(): void {
    if (!this.userId || !this.newEmail || !this.emailOtp) return;
    this.resetEmailMessages();
    this.emailVerifying = true;
    const req: EmailChangeVerifyRequest = { userId: this.userId, newEmail: this.newEmail, code: this.emailOtp.trim() };
    this.selfService.verifyEmailChange(req).subscribe({
      next: () => {
        this.emailSuccess = 'Email updated successfully.';
        this.emailVerifying = false;
        this.emailOtpSent = false;
        this.emailOtp = '';
      },
      error: (err) => {
        this.emailError = err?.error?.message || 'Email OTP verification failed.';
        this.emailVerifying = false;
      }
    });
  }

  initiatePhone(): void {
    if (!this.userId || !this.newPhone) return;
    this.resetPhoneMessages();
    this.phoneInitiating = true;
    const req: PhoneChangeInitiateRequest = { userId: this.userId, newPhone: this.newPhone };
    this.selfService.initiatePhoneChange(req).subscribe({
      next: () => {
        this.phoneOtpSent = true;
        this.phoneSuccess = 'OTP sent to your registered email.';
        this.phoneInitiating = false;
      },
      error: (err) => {
        this.phoneError = err?.error?.message || 'Failed to send OTP for phone update.';
        this.phoneInitiating = false;
      }
    });
  }

  verifyPhone(): void {
    if (!this.userId || !this.newPhone || !this.phoneOtp) return;
    this.resetPhoneMessages();
    this.phoneVerifying = true;
    const req: PhoneChangeVerifyRequest = { userId: this.userId, newPhone: this.newPhone, code: this.phoneOtp.trim() };
    this.selfService.verifyPhoneChange(req).subscribe({
      next: () => {
        this.phoneSuccess = 'Phone updated successfully.';
        this.phoneVerifying = false;
        this.phoneOtpSent = false;
        this.phoneOtp = '';
      },
      error: (err) => {
        this.phoneError = err?.error?.message || 'Phone OTP verification failed.';
        this.phoneVerifying = false;
      }
    });
  }

  private resetEmailMessages() {
    this.emailError = null; this.emailSuccess = null;
  }
  private resetPhoneMessages() {
    this.phoneError = null; this.phoneSuccess = null;
  }
}
