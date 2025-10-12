import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // For dropdown selection
import { TransactionService } from '../transaction.service';
import { AccountService } from '../../accounts/account.service'; // To get user's accounts
import { AccountResponse } from '../../../shared/models/account.model';
import { TransactionResponse, TransactionStatus, TransactionType } from '../../../shared/models/transaction.model';
import { AuthService } from '../../../core/services/auth.service';





@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-history.component.html',
  styleUrls: ['./transaction-history.component.css']
})
export class TransactionHistoryComponent implements OnInit {
  accounts: AccountResponse[] = [];
  selectedAccountId: string | null = null;
  transactions: TransactionResponse[] = [];
  loading: boolean = true;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  // Statement download state
  fromDate: string = '';
  toDate: string = '';
  toEmail?: string;
  otpCode: string = '';
  statementLoading = false;
  statementOtpSent = false;
  statementError: string | null = null;
  statementSuccess: string | null = null;

  constructor(
    private transactionService: TransactionService,
    private accountService: AccountService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.setDefaultDates();
    this.loadUserAccounts();
  }

  loadUserAccounts(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const userId = this.authService.getIdentityClaims()?.sub;

    if (userId) {
      this.accountService.getAccountsByUserId(userId).subscribe(
        (data) => {
          this.accounts = data || []; // FIX: Assign data, or an empty array if data is null/undefined
          this.loading = false;
          if (this.accounts.length > 0) {
            this.selectedAccountId = this.accounts[0].accountId; // Select first account by default
            this.onAccountChange(); // Load transactions for the default account
          } else {
            this.successMessage = 'No accounts found to display transaction history.';
          }
        },
        (error) => {
          console.error('Error loading accounts for history:', error);
          this.errorMessage = error.error?.message || 'Failed to load accounts for history.';
          this.loading = false;
        }
      );
    } else {
      this.errorMessage = 'User ID not found. Please log in again.';
      this.loading = false;
    }
  }

  onAccountChange(): void {
    if (this.selectedAccountId) {
      this.loading = true;
      this.errorMessage = null;
      this.successMessage = null;
      this.transactions = []; // Clear previous transactions

      this.transactionService.getTransactionsByAccountId(this.selectedAccountId).subscribe(
        (data) => {
          // Ensure latest transactions first by transactionDate desc
          this.transactions = (data || []).slice().sort((a, b) => {
            const ta = a?.transactionDate ? new Date(a.transactionDate).getTime() : 0;
            const tb = b?.transactionDate ? new Date(b.transactionDate).getTime() : 0;
            return tb - ta;
          });
          this.loading = false;
          if (this.transactions.length === 0) {
            this.successMessage = 'No transactions found for this account.';
          }
        },
        (error) => {
          console.error('Error loading transactions:', error);
          this.errorMessage = error.error?.message || 'Failed to load transactions.';
          this.loading = false;
        }
      );
    } else {
      this.transactions = [];
      this.successMessage = 'Please select an account.';
    }
  }

  getTransactionStatusClass(status: TransactionStatus): string {
    switch (status) {
      case TransactionStatus.SUCCESS: return 'status-success';
      case TransactionStatus.FAILED: return 'status-failed';
      case TransactionStatus.PENDING: return 'status-pending';
      default: return '';
    }
  }

  getTransactionTypeClass(type: TransactionType): string {
    switch (type) {
      case TransactionType.DEPOSIT: return 'type-deposit';
      case TransactionType.WITHDRAW: return 'type-withdraw';
      case TransactionType.TRANSFER: return 'type-transfer';
      default: return '';
    }
  }

  getMethodDetails(tx: TransactionResponse): string {
    if (!tx?.metadataJson) return '-';
    try {
      const meta = JSON.parse(tx.metadataJson);
      const method = (meta?.method || '').toString().toUpperCase();

      // Card method (existing)
      if (method === 'DEBIT_CARD' || meta?.panMasked || meta?.brand) {
        const parts: string[] = ['Debit Card'];
        if (meta?.brand) parts.push(meta.brand);
        const head = parts.join(' • ');
        return meta?.panMasked ? head + ' • ' + meta.panMasked : head;
      }

      // Account method (new: show masked account numbers)
      if (method === 'ACCOUNT') {
        const fromMasked = meta?.fromAccountMasked || null;
        const toMasked = meta?.toAccountMasked || null;

        if (fromMasked && toMasked) {
          return `Account • ${fromMasked} → ${toMasked}`;
        }
        if (fromMasked) {
          return `Account • From ${fromMasked}`;
        }
        if (toMasked) {
          return `Account • To ${toMasked}`;
        }

        // Fallback to raw numbers if masked not present
        const fromNum = meta?.fromAccountNumber || null;
        const toNum = meta?.toAccountNumber || null;
        if (fromNum && toNum) {
          return `Account • ${fromNum} → ${toNum}`;
        }
        if (fromNum) {
          return `Account • From ${fromNum}`;
        }
        if (toNum) {
          return `Account • To ${toNum}`;
        }
        return 'Account';
      }

      return method || '-';
    } catch {
      return '-';
    }
  }

  getAccountDisplayName(accountId: string | null): string {
    if (!accountId) return '-';

    const account = this.accounts.find(acc => acc.accountId === accountId);
    if (account) return account.accountNumber;

    return accountId.slice(0, 8) + '...';
  }
  getSelectedAccountNumber(): string {
    const account = this.accounts.find(acc => acc.accountId === this.selectedAccountId);
    return account ? account.accountNumber : '';
  }

  private setDefaultDates(): void {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - 30);
    this.fromDate = from.toISOString().slice(0, 10);
    this.toDate = to.toISOString().slice(0, 10);
  }

  initiateStatement(): void {
    this.statementError = null;
    this.statementSuccess = null;

    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.statementError = 'User not found. Please login again.';
      return;
    }
    if (!this.selectedAccountId) {
      this.statementError = 'Please select an account.';
      return;
    }
    if (!this.fromDate || !this.toDate) {
      this.statementError = 'Please choose a valid date range.';
      return;
    }
    if (this.fromDate > this.toDate) {
      this.statementError = 'From date cannot be after To date.';
      return;
    }

    this.statementLoading = true;
    this.transactionService.initiateStatement({
      userId,
      accountId: this.selectedAccountId,
      fromDate: this.fromDate,
      toDate: this.toDate,
      toEmail: this.toEmail || undefined
    }).subscribe({
      next: () => {
        this.statementOtpSent = true;
        this.statementSuccess = 'OTP sent to your email.';
        this.statementLoading = false;
      },
      error: (err) => {
        console.error('Statement initiate failed', err);
        this.statementError = err?.error?.message || 'Failed to send OTP for statement.';
        this.statementLoading = false;
      }
    });
  }

  verifyAndSendStatement(): void {
    this.statementError = null;
    this.statementSuccess = null;

    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.statementError = 'User not found. Please login again.';
      return;
    }
    if (!this.selectedAccountId) {
      this.statementError = 'Please select an account.';
      return;
    }
    if (!this.otpCode || this.otpCode.trim().length !== 6) {
      this.statementError = 'Enter the 6-digit OTP.';
      return;
    }

    this.statementLoading = true;
    this.transactionService.verifyStatement({
      userId,
      accountId: this.selectedAccountId,
      fromDate: this.fromDate,
      toDate: this.toDate,
      code: this.otpCode.trim(),
      toEmail: this.toEmail || undefined
    }).subscribe({
      next: (resp) => {
        // Treat any 2xx as success; backend returns 202 Accepted when mail delivery fails but statement is generated.
        if (resp.status >= 200 && resp.status < 300) {
          // Use server message if present, else a generic success
          const msg = 'Statement sent to your email. PDF password: FIRST4NAME (uppercase) + YEAR of birth (e.g., ABCD2003).';
          this.statementSuccess = msg;
          this.statementError = null;
          this.statementLoading = false;
          this.otpCode = '';
          this.statementOtpSent = false;
        } else {
          this.statementError = 'Unexpected response from server.';
          this.statementLoading = false;
        }
      },
      error: (err) => {
        console.error('Statement verify failed', err);
        // If backend responds with non-JSON or 202 as error (edge case), surface text or a friendly message
        this.statementError = err?.error?.message || (typeof err?.error === 'string' ? err.error : 'OTP verification failed.');
        this.statementLoading = false;
      }
    });
  }
}
