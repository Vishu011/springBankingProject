// src/app/features/cards/card-issuance/card-issuance.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { CardService } from '../card.service';
import { AccountService } from '../../accounts/account.service';
import { AuthService } from '../../../core/services/auth.service';
import { OtpService, GenerateOtpRequest } from '../../otp/otp.service';

import { AccountResponse, AccountType } from '../../../shared/models/account.model';
import { CardApplicationResponse, CardBrand, CardKind, CreateCardApplicationRequestDto } from '../../../shared/models/card.model';

@Component({
  selector: 'app-card-issuance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card-issuance.component.html',
  styleUrls: ['./card-issuance.component.css']
})
export class CardIssuanceComponent implements OnInit {
  accounts: AccountResponse[] = [];

  // Application form (no limit/dates; admin sets them)
  applicationForm: CreateCardApplicationRequestDto = {
    userId: '',
    accountId: '',
    type: CardKind.CREDIT,
    requestedBrand: CardBrand.VISA,
    otpCode: ''
  };

  // Brand options filtered by selected account's type
  allowedBrands: CardBrand[] = [];

  CardKind = CardKind;
  CardBrand = CardBrand;
  AccountType = AccountType;

  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private cardService: CardService,
    private accountService: AccountService,
    private authService: AuthService,
    private otpService: OtpService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User ID not found. Please log in again.';
      return;
    }
    this.applicationForm.userId = userId;
    this.loadUserAccounts(userId);
  }

  private loadUserAccounts(userId: string): void {
    this.loading = true;
    this.accountService.getAccountsByUserId(userId).subscribe({
      next: (data) => {
        this.accounts = (data || []).filter(acc => acc.status === 'ACTIVE');
        if (this.accounts.length === 0) {
          this.errorMessage = 'No active accounts found. Please create an account first.';
          this.loading = false;
          return;
        }
        // Default select first account
        this.applicationForm.accountId = this.accounts[0].accountId;
        this.refreshAllowedBrands();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading accounts:', err);
        this.errorMessage = err?.error?.message || 'Failed to load accounts.';
        this.loading = false;
      }
    });
  }

  onAccountChange(): void {
    this.refreshAllowedBrands();
  }

  onTypeChange(): void {
    // No brand list change by type; brand constraints are by account type
    // Keep method if future logic needs it
  }

  private refreshAllowedBrands(): void {
    const acc = this.accounts.find(a => a.accountId === this.applicationForm.accountId);
    if (!acc) {
      this.allowedBrands = [];
      return;
    }
    this.allowedBrands = this.allowedBrandsForAccountType(acc.accountType);

    // Ensure currently selected brand is within allowed brands
    if (!this.allowedBrands.includes(this.applicationForm.requestedBrand)) {
      this.applicationForm.requestedBrand = this.allowedBrands[0];
    }
  }

  private allowedBrandsForAccountType(accountType: AccountType): CardBrand[] {
    if (accountType === AccountType.SALARY_CORPORATE) {
      return [CardBrand.AMEX, CardBrand.MASTERCARD, CardBrand.DISCOVERY];
    }
    // SAVINGS
    return [CardBrand.VISA, CardBrand.RUPAY];
  }

  generateOtp(): void {
    this.errorMessage = null;
    this.successMessage = null;
    const userId = this.applicationForm.userId;
    if (!userId || !this.applicationForm.accountId) {
      this.errorMessage = 'Select an account first.';
      return;
    }
    const req: GenerateOtpRequest = {
      userId,
      purpose: 'CARD_ISSUANCE',
      channels: ['EMAIL'],
      contextId: this.applicationForm.accountId
    };
    this.otpService.generate(req).subscribe({
      next: () => {
        this.successMessage = 'OTP sent to your registered email.';
      },
      error: (err) => {
        console.error('Failed to generate OTP', err);
        this.errorMessage = err?.error?.message || 'Failed to generate OTP.';
      }
    });
  }

  onSubmit(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    if (!this.applicationForm.userId || !this.applicationForm.accountId) {
      this.errorMessage = 'User ID or Account ID is missing.';
      this.loading = false;
      return;
    }
    if (!this.applicationForm.otpCode || this.applicationForm.otpCode.trim().length === 0) {
      this.errorMessage = 'Please enter the OTP sent to your email.';
      this.loading = false;
      return;
    }

    this.cardService.submitApplication(this.applicationForm).subscribe({
      next: (resp: CardApplicationResponse) => {
        this.successMessage = `Application submitted for ${resp.requestedBrand} ${resp.type}. Status: ${resp.status}.`;
        this.loading = false;
        // Reset only OTP, keep selections for another application if needed
        this.applicationForm.otpCode = '';
        // Navigate to manage cards or applications list (manage for now)
        this.router.navigate(['/cards/manage']);
      },
      error: (err) => {
        console.error('Application submission failed:', err);
        this.errorMessage = err?.error?.message || 'Application submission failed.';
        this.loading = false;
      }
    });
  }
}
