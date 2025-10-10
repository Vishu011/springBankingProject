// src/app/features/transactions/withdraw/withdraw.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../transaction.service';
import { AccountService } from '../../accounts/account.service';
import { AccountResponse } from '../../../shared/models/account.model';
import { AuthService } from '../../../core/services/auth.service';
import { WithdrawRequest, DebitCardWithdrawRequest } from '../../../shared/models/transaction.model';
import { OtpService, GenerateOtpRequest } from '../../otp/otp.service';


@Component({
  selector: 'app-withdraw',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './withdraw.component.html',
  styleUrls: ['./withdraw.component.css']
})
export class WithdrawComponent implements OnInit {
  accounts: AccountResponse[] = [];

  // Mode: withdraw from account or debit card
  mode: 'ACCOUNT' | 'DEBIT_CARD' = 'ACCOUNT';

  // Account withdraw form
  withdrawForm = {
    accountId: '',
    amount: 0,
    otpCode: ''
  };

  // Debit card withdraw form
  debitCardForm: DebitCardWithdrawRequest = {
    cardNumber: '',
    cvv: '',
    amount: 0,
    otpCode: ''
  };

  
  loadingAccounts: boolean = true;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  submitting: boolean = false;
  genOtpLoading: boolean = false;
  otpCooldownSec: number = 0;
  private otpCooldownTimer: any = null;

  constructor(
    private transactionService: TransactionService,
    private accountService: AccountService,
    private authService: AuthService,
    private otpService: OtpService
  ) { }

  ngOnInit(): void {
    this.loadUserAccounts();
  }

  loadUserAccounts(): void {
    this.loadingAccounts = true;
    this.errorMessage = null;

    const userId = this.authService.getIdentityClaims()?.sub;

    if (userId) {
      this.accountService.getAccountsByUserId(userId).subscribe(
        (data) => {
          this.accounts = data.filter(acc => acc.status === 'ACTIVE'); // Only active accounts
          this.loadingAccounts = false;
          if (this.accounts.length > 0) {
            this.withdrawForm.accountId = this.accounts[0].accountId; // Select first active account by default
          } else {
            this.errorMessage = 'No active accounts found to withdraw from.';
          }
        },
        (error) => {
          console.error('Error loading accounts for withdrawal:', error);
          this.errorMessage = error.error?.message || 'Failed to load accounts.';
          this.loadingAccounts = false;
        }
      );
    } else {
      this.errorMessage = 'User ID not found. Please log in again.';
      this.loadingAccounts = false;
    }
  }

  onSubmit(): void {
    this.errorMessage = null;
    this.successMessage = null;

    if (this.mode === 'ACCOUNT') {
      const { accountId, amount } = this.withdrawForm;

      if (!accountId) {
        this.errorMessage = 'Please select an account.';
        return;
      }
      if (amount <= 0) {
        this.errorMessage = 'Withdrawal amount must be positive.';
        return;
      }
      const selectedAccount = this.accounts.find(acc => acc.accountId === accountId);
      if (selectedAccount && selectedAccount.balance < amount) {
        this.errorMessage = 'Insufficient funds in the selected account.';
        return;
      }
      if (!this.withdrawForm.otpCode) {
        this.errorMessage = 'Please enter the OTP sent to your email.';
        return;
      }

      const request: WithdrawRequest = {
        accountId,
        amount,
        otpCode: this.withdrawForm.otpCode
      };

      this.submitting = true;
      this.transactionService.withdrawFunds(request).subscribe(
        (response) => {
          this.successMessage = `Withdrawal of ₹${response.amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} completed from account ending ${response.fromAccountId?.slice(-4)}. Transaction ID: ${response.transactionId}.`;
          this.resetForm();
          this.submitting = false;
          if (response.fromAccountId) {
            this.accountService.getAccountById(response.fromAccountId).subscribe(
              acc => {
                const updatedAccount = this.accounts.find(a => a.accountId === acc.accountId);
                if (updatedAccount) updatedAccount.balance = acc.balance;
              }
            );
          }
        },
        (error) => {
          console.error('Withdrawal failed:', error);
          this.errorMessage = error.error?.message || 'Withdrawal failed. Please try again.';
          this.submitting = false;
        }
      );
    } else {
      // DEBIT CARD mode
      const { cardNumber, cvv, amount, otpCode } = this.debitCardForm;

      if (!cardNumber || cardNumber.trim().length < 12) {
        this.errorMessage = 'Enter a valid debit card number.';
        return;
      }
      if (!cvv || cvv.trim().length < 3) {
        this.errorMessage = 'Enter a valid CVV.';
        return;
      }
      if (amount <= 0) {
        this.errorMessage = 'Withdrawal amount must be positive.';
        return;
      }
      if (!otpCode) {
        this.errorMessage = 'Please enter the OTP sent to your email.';
        return;
      }

      this.submitting = true;
      this.transactionService.debitCardWithdraw(this.debitCardForm).subscribe(
        (response) => {
          // metadataJson can hold panMasked and brand
          let meta: any = {};
          try { meta = response.metadataJson ? JSON.parse(response.metadataJson) : {}; } catch {}
          const panMasked = meta?.panMasked ? ` using card ${meta.panMasked}` : '';
          this.successMessage = `Debit card withdrawal of ₹${response.amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}${panMasked} succeeded. Transaction ID: ${response.transactionId}.`;
          this.resetForm();
          this.submitting = false;
        },
        (error) => {
          console.error('Debit card withdrawal failed:', error);
          this.errorMessage = error.error?.message || 'Debit card withdrawal failed. Please try again.';
          this.submitting = false;
        }
      );
    }
  }

  resetForm(): void {
    this.withdrawForm = {
      accountId: this.accounts.length > 0 ? this.accounts[0].accountId : '',
      amount: 0,
      otpCode: ''
    };
    this.debitCardForm = {
      cardNumber: '',
      cvv: '',
      amount: 0,
      otpCode: ''
    };
  }

  private startOtpCooldown(seconds: number = 60): void {
    this.otpCooldownSec = seconds;
    if (this.otpCooldownTimer) {
      clearInterval(this.otpCooldownTimer);
    }
    this.otpCooldownTimer = setInterval(() => {
      if (this.otpCooldownSec > 0) {
        this.otpCooldownSec--;
      } else {
        clearInterval(this.otpCooldownTimer);
        this.otpCooldownTimer = null;
      }
    }, 1000);
  }

  generateOtp(): void {
    this.errorMessage = null;
    this.successMessage = null;
    if (this.genOtpLoading || this.otpCooldownSec > 0) {
      return;
    }
    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User ID not found. Please log in again.';
      return;
    }
    this.genOtpLoading = true;
    const req: GenerateOtpRequest = {
      userId,
      purpose: 'WITHDRAWAL',
      channels: ['EMAIL'],
      contextId: this.mode === 'ACCOUNT' ? (this.withdrawForm.accountId || null) : null
    };
    this.otpService.generate(req).subscribe({
      next: () => {
        this.successMessage = 'OTP has been sent to your registered email.';
        this.startOtpCooldown(60);
        this.genOtpLoading = false;
      },
      error: (err) => {
        console.error('Failed to generate OTP', err);
        this.errorMessage = err.error?.message || 'Failed to generate OTP. Please try again.';
        this.genOtpLoading = false;
      }
    });
  }
}
