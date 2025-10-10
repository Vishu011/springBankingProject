// src/app/features/accounts/accounts-list/accounts-list.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // For ngIf, ngFor
import { FormsModule } from '@angular/forms'; // For ngModel (if using forms for creation/updates)
import { AccountService } from '../account.service'; // Import AccountService
// Import AuthService

import { UserProfileService, UserProfile } from '../../user-profile/user-profile.service'; // Import UserProfileService
import { AuthService } from '../../../core/services/auth.service';
import { AccountCreationRequest, AccountResponse, AccountStatus, AccountType } from '../../../shared/models/account.model';
import { OtpService } from '../../otp/otp.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-accounts-list',
  standalone: true,
  imports: [CommonModule, FormsModule], // Include FormsModule for form interactions
  templateUrl: './accounts-list.component.html',
  styleUrls: ['./accounts-list.component.css']
})
export class AccountsListComponent implements OnInit {
  accounts: AccountResponse[] = [];
  loading: boolean = true;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  // For new account creation form
  newAccount: AccountCreationRequest = {
    userId: '', // Will be populated from logged-in user
    accountType: AccountType.SAVINGS, // Default type
    initialBalance: 0,
    otpCode: '' // OTP required by backend for ACCOUNT_OPERATION
  };
  accountTypes = Object.values(AccountType); // For dropdown

  // Row-wise OTPs for account operations (block/unblock/delete)
  rowOtpCodes: { [accountId: string]: string } = {};
  generatingCreateOtp = false;
  generatingRowOtp: { [accountId: string]: boolean } = {};

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private userProfileService: UserProfileService, // Inject UserProfileService to get current user's ID
    private otpService: OtpService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadUserAccounts();
  }

  loadUserAccounts(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const userId = this.authService.getIdentityClaims()?.sub; // Get user ID from JWT

    if (userId) {
      this.accountService.getAccountsByUserId(userId).subscribe(
        (data) => {
          this.accounts = Array.isArray(data) ? data : [];
          this.loading = false;
          if (this.accounts.length === 0) {
            this.successMessage = 'You have no accounts yet. Create one!';
          }
        },
        (error) => {
          console.error('Error loading accounts:', error);
          this.errorMessage = error.error?.message || 'Failed to load accounts.';
          this.loading = false;
        }
      );
    } else {
      this.errorMessage = 'User ID not found. Please log in again.';
      this.loading = false;
    }
  }

  generateOtpForAccountCreation(): void {
    this.errorMessage = null;
    this.successMessage = null;
    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User not logged in. Cannot generate OTP.';
      return;
    }
    this.generatingCreateOtp = true;
    this.otpService.generate({
      userId,
      purpose: 'ACCOUNT_OPERATION',
      channels: ['EMAIL'],
      contextId: null
    }).subscribe(
      () => {
        this.successMessage = 'OTP sent to your registered email.';
        this.generatingCreateOtp = false;
      },
      (error) => {
        console.error('Error generating OTP for account creation:', error);
        this.errorMessage = error.error?.message || 'Failed to generate OTP.';
        this.generatingCreateOtp = false;
      }
    );
  }

  generateOtpForAccountOperation(accountId: string): void {
    this.errorMessage = null;
    this.successMessage = null;
    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User not logged in. Cannot generate OTP.';
      return;
    }
    this.generatingRowOtp[accountId] = true;
    this.otpService.generate({
      userId,
      purpose: 'ACCOUNT_OPERATION',
      channels: ['EMAIL'],
      contextId: null
    }).subscribe(
      () => {
        this.successMessage = 'OTP sent to your registered email.';
        this.generatingRowOtp[accountId] = false;
      },
      (error) => {
        console.error('Error generating OTP for account operation:', error);
        this.errorMessage = error.error?.message || 'Failed to generate OTP.';
        this.generatingRowOtp[accountId] = false;
      }
    );
  }

  createAccount(): void {
    this.errorMessage = null;
    this.successMessage = null;
    const userId = this.authService.getIdentityClaims()?.sub;

    if (!userId) {
      this.errorMessage = 'User not logged in. Cannot create account.';
      return;
    }

    // Redirect Salary/Corporate flow to application UI
    if (this.newAccount.accountType === AccountType.SALARY_CORPORATE) {
      this.router.navigate(['/accounts/salary/apply']);
      return;
    }

    if (!this.newAccount.otpCode || this.newAccount.otpCode.trim().length === 0) {
      this.errorMessage = 'Please enter the OTP received to proceed.';
      return;
    }

    // Optional client-side validation for Savings initial deposit range
    if (this.newAccount.accountType === AccountType.SAVINGS) {
      if (this.newAccount.initialBalance < 2000 || this.newAccount.initialBalance > 200000) {
        this.errorMessage = 'Savings initial deposit must be between 2000 and 200000.';
        return;
      }
    }

    this.newAccount.userId = userId; // Assign logged-in user's ID

    this.accountService.createAccount(this.newAccount).subscribe(
      (response) => {
        this.successMessage = `Account ${response.accountNumber} created successfully!`;
        this.loadUserAccounts(); // Reload accounts list
        // Reset form
        this.newAccount = { userId: userId, accountType: AccountType.SAVINGS, initialBalance: 0, otpCode: '' };
      },
      (error) => {
        console.error('Error creating account:', error);
        this.errorMessage = error.error?.message || 'Failed to create account.';
      }
    );
  }

  blockAccount(accountId: string): void {
    const otp = this.rowOtpCodes[accountId];
    if (!otp || otp.trim().length === 0) {
      this.errorMessage = 'Please enter OTP to block the account.';
      return;
    }
    if (confirm(`Are you sure you want to BLOCK account ${accountId}?`)) {
      this.accountService.updateAccountStatus(accountId, AccountStatus.BLOCKED, otp).subscribe(
        (response) => {
          this.successMessage = `Account ${response.accountNumber} blocked successfully.`;
          this.rowOtpCodes[accountId] = '';
          this.loadUserAccounts();
        },
        (error) => {
          console.error('Error blocking account:', error);
          this.errorMessage = error.error?.message || 'Failed to block account.';
        }
      );
    }
  }

  unblockAccount(accountId: string): void {
    const otp = this.rowOtpCodes[accountId];
    if (!otp || otp.trim().length === 0) {
      this.errorMessage = 'Please enter OTP to unblock the account.';
      return;
    }
    if (confirm(`Are you sure you want to UNBLOCK account ${accountId}?`)) {
      this.accountService.updateAccountStatus(accountId, AccountStatus.ACTIVE, otp).subscribe(
        (response) => {
          this.successMessage = `Account ${response.accountNumber} unblocked successfully.`;
          this.rowOtpCodes[accountId] = '';
          this.loadUserAccounts();
        },
        (error) => {
          console.error('Error unblocking account:', error);
          this.errorMessage = error.error?.message || 'Failed to unblock account.';
        }
      );
    }
  }

  deleteAccount(accountId: string): void {
    const otp = this.rowOtpCodes[accountId];
    if (!otp || otp.trim().length === 0) {
      this.errorMessage = 'Please enter OTP to delete the account.';
      return;
    }
    if (confirm(`Are you sure you want to DELETE account ${accountId}? This action cannot be undone.`)) {
      this.accountService.deleteAccount(accountId, otp).subscribe(
        () => {
          this.successMessage = `Account ${accountId} deleted successfully.`;
          this.rowOtpCodes[accountId] = '';
          this.loadUserAccounts();
        },
        (error) => {
          console.error('Error deleting account:', error);
          this.errorMessage = error.error?.message || 'Failed to delete account.';
        }
      );
    }
  }

  goToSalaryApplication(): void {
    this.router.navigate(['/accounts/salary/apply']);
  }

  getAccountStatusClass(status: AccountStatus): string {
    switch (status) {
      case AccountStatus.ACTIVE: return 'status-active';
      case AccountStatus.BLOCKED: return 'status-blocked';
      case AccountStatus.CLOSED: return 'status-closed';
      default: return '';
    }
  }
}
