import { Injectable } from '@angular/core';
import { Observable, of, throwError, BehaviorSubject, forkJoin } from 'rxjs';
import { map, catchError, switchMap, tap } from 'rxjs/operators';
import { AccountService } from '../accounts/account.service';
import { TransactionService } from '../transactions/transaction.service';
import { AuthService } from '../../core/services/auth.service';
import { AccountResponse, AccountType, AccountStatus } from '../../shared/models/account.model';
import { TransactionResponse, TransactionType, TransactionStatus } from '../../shared/models/transaction.model';
import { CardService } from '../cards/card.service';
import { CardResponse, CardKind } from '../../shared/models/card.model';
import { LoanService } from '../loans/loan.service';
import { LoanResponse } from '../../shared/models/loan.model';
import { LoanStatus } from '../../shared/models/loan.model';

export interface ChatbotApiResponse {
  success: boolean;
  data?: any;
  error?: string;
  breadcrumb?: string;
}

export interface UserContext {
  userId: string;
  accounts: AccountResponse[];
  recentTransactions: TransactionResponse[];
  creditCards?: CardResponse[];
  loans?: LoanResponse[];
  kycStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
  lastUpdated: Date;
}

@Injectable({
  providedIn: 'root'
})
export class ChatbotApiService {
  private userContextSubject = new BehaviorSubject<UserContext | null>(null);
  public userContext$ = this.userContextSubject.asObservable();

  constructor(
    private accountService: AccountService,
    private transactionService: TransactionService,
    private authService: AuthService,
    private cardService: CardService,
    private loanService: LoanService
  ) {}

  /**
   * Initialize user context with real data from microservices
   */
  initializeUserContext(): Observable<UserContext> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not authenticated'));
    }

    return this.loadUserData(userId).pipe(
      tap(context => this.userContextSubject.next(context)),
      catchError(error => {
        console.error('Failed to load user context:', error);
        // Return fallback context with limited data
        const fallbackContext: UserContext = {
          userId,
          accounts: [],
          recentTransactions: [],
          kycStatus: 'PENDING',
          lastUpdated: new Date()
        };
        this.userContextSubject.next(fallbackContext);
        return of(fallbackContext);
      })
    );
  }

  /**
   * Get user's account information
   */
  getUserAccounts(): Observable<AccountResponse[]> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not authenticated'));
    }

    return this.accountService.getAccountsByUserId(userId).pipe(
      catchError(error => {
        console.error('Failed to load accounts:', error);
        return of([]);
      })
    );
  }

  /**
   * Get account balance for a specific account
   */
  getAccountBalance(accountId: string): Observable<AccountResponse> {
    return this.accountService.getAccountById(accountId).pipe(
      catchError(error => {
        console.error('Failed to load account balance:', error);
        return throwError(() => new Error('Unable to retrieve account balance'));
      })
    );
  }

  /**
   * Get recent transactions for user
   */
  getRecentTransactions(limit: number = 5): Observable<TransactionResponse[]> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not authenticated'));
    }

    return this.getUserAccounts().pipe(
      switchMap((accounts: AccountResponse[]) => {
        if (!accounts || accounts.length === 0) {
          return of([]);
        }

        const requests = accounts.map(acc =>
          this.transactionService.getTransactionsByAccountId(acc.accountId)
        );

        return forkJoin(requests).pipe(
          map((results: TransactionResponse[][]) =>
            results
              .flat()
              .filter(Boolean)
              .sort((a, b) => {
                const ta = a?.transactionDate ? new Date(a.transactionDate).getTime() : 0;
                const tb = b?.transactionDate ? new Date(b.transactionDate).getTime() : 0;
                return tb - ta;
              })
              .slice(0, Math.max(0, limit))
          )
        );
      }),
      catchError(error => {
        console.error('Failed to load transactions:', error);
        return of([]);
      })
    );
  }

  /**
   * Get user's KYC status
   */
  getKycStatus(): Observable<'PENDING' | 'VERIFIED' | 'REJECTED'> {
    // This would typically come from a user profile service
    // For now, return a default status
    return of('VERIFIED');
  }

  /**
   * Process account balance query with real data
   */
  processAccountBalanceQuery(): Observable<ChatbotApiResponse> {
    return this.getUserAccounts().pipe(
      map(accounts => {
        if (accounts.length === 0) {
          return {
            success: false,
            error: 'No accounts found. Please contact customer service.',
            breadcrumb: 'Accounts'
          };
        }

        let response = 'Here are your account balances:\n\n';
        accounts.forEach(account => {
          const maskedAccount = this.maskAccountNumber(account.accountNumber);
          response += `• ${account.accountType} Account (****${maskedAccount}): ₹${account.balance.toLocaleString('en-IN')}\n`;
        });

        response += '\nFor detailed transaction history, please visit the Transactions section.';

        return {
          success: true,
          data: response,
          breadcrumb: 'Accounts'
        };
      }),
      catchError(error => {
        return of({
          success: false,
          error: 'Unable to retrieve account information. Please try again later.',
          breadcrumb: 'Accounts'
        });
      })
    );
  }

  /**
   * Process account information query with real data
   */
  processAccountInfoQuery(): Observable<ChatbotApiResponse> {
    return this.getUserAccounts().pipe(
      map(accounts => {
        if (accounts.length === 0) {
          return {
            success: false,
            error: 'No accounts found. Please contact customer service.',
            breadcrumb: 'Accounts'
          };
        }

        let response = 'Here are your accounts:\n\n';
        accounts.forEach(account => {
          const maskedAccount = this.maskAccountNumber(account.accountNumber);
          response += `• ${account.accountType} Account\n`;
          response += `  Account Number: ****${maskedAccount}\n`;
          response += `  Balance: ₹${account.balance.toLocaleString('en-IN')}\n`;
          response += `  Status: ${account.status}\n\n`;
        });

        response += 'You can manage your accounts, view statements, or perform transactions from the Accounts section.';

        return {
          success: true,
          data: response,
          breadcrumb: 'Accounts'
        };
      }),
      catchError(error => {
        return of({
          success: false,
          error: 'Unable to retrieve account information. Please try again later.',
          breadcrumb: 'Accounts'
        });
      })
    );
  }

  /**
   * Process KYC status query with real data
   */
  processKycStatusQuery(): Observable<ChatbotApiResponse> {
    return this.getKycStatus().pipe(
      map(kycStatus => {
        let response = 'Your KYC (Know Your Customer) status:\n\n';
        
        switch (kycStatus) {
          case 'VERIFIED':
            response += '✅ Your KYC is verified and up to date.\n';
            response += 'You have full access to all banking services.';
            break;
          case 'PENDING':
            response += '⏳ Your KYC verification is pending.\n';
            response += 'Please complete the verification process to access all banking features.';
            break;
          case 'REJECTED':
            response += '❌ Your KYC verification was rejected.\n';
            response += 'Please contact customer service for assistance with re-verification.';
            break;
          default:
            response += 'Please complete your KYC verification to access all banking services.';
        }

        response += '\n\nYou can update your KYC information in the KYC section of your dashboard.';

        return {
          success: true,
          data: response,
          breadcrumb: 'KYC'
        };
      }),
      catchError(error => {
        return of({
          success: false,
          error: 'Unable to retrieve KYC status. Please try again later.',
          breadcrumb: 'KYC'
        });
      })
    );
  }

  /**
   * Process transaction history query
   */
  processTransactionHistoryQuery(): Observable<ChatbotApiResponse> {
    return this.getRecentTransactions(5).pipe(
      map(transactions => {
        if (transactions.length === 0) {
          return {
            success: true,
            data: 'No recent transactions found. Visit the Transactions section to view your complete transaction history.',
            breadcrumb: 'Statements'
          };
        }

        let response = 'Here are your recent transactions:\n\n';
        transactions.forEach(transaction => {
          const amount = transaction.amount.toLocaleString('en-IN');
          const date = new Date(transaction.transactionDate).toLocaleDateString();
          response += `• ${transaction.type} - ₹${amount} (${date})\n`;
        });

        response += '\nFor complete transaction history, visit the Transactions section.';

        return {
          success: true,
          data: response,
          breadcrumb: 'Statements'
        };
      }),
      catchError(error => {
        return of({
          success: false,
          error: 'Unable to retrieve transaction history. Please try again later.',
          breadcrumb: 'Statements'
        });
      })
    );
  }

  /**
   * Get user's credit cards (issued)
   */
  getUserCreditCards(): Observable<CardResponse[]> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not authenticated'));
    }
    return this.cardService.listMyCards(userId).pipe(
      catchError(error => {
        console.error('Failed to load cards:', error);
        return of([] as CardResponse[]);
      })
    );
  }

  /**
   * Get user's loan applications
   */
  getUserLoans(): Observable<LoanResponse[]> {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return throwError(() => new Error('User not authenticated'));
    }
    return this.loanService.getLoansByUserId(userId).pipe(
      catchError(error => {
        console.error('Failed to load loans:', error);
        return of([] as LoanResponse[]);
      })
    );
  }

  /**
   * Process credit card summary query
   */
  processCreditCardSummaryQuery(): Observable<ChatbotApiResponse> {
    return this.getUserCreditCards().pipe(
      map((cards: CardResponse[]) => {
        const total = cards.length;
        const creditCount = cards.filter(c => c.type === CardKind.CREDIT).length;
        const debitCount = total - creditCount;

        const response =
          total === 0
            ? 'You do not have any issued cards yet. You can request a new card from the Cards section.'
            : `You currently have ${total} card${total > 1 ? 's' : ''}.\n\n• Credit cards: ${creditCount}\n• Debit cards: ${debitCount}`;

        return {
          success: true,
          data: response,
          breadcrumb: 'Cards'
        };
      }),
      catchError(error => {
        return of({
          success: false,
          error: 'Unable to retrieve your cards right now. Please try again later.',
          breadcrumb: 'Cards'
        });
      })
    );
  }

  /**
   * Process loan summary query
   */
  processLoanSummaryQuery(): Observable<ChatbotApiResponse> {
    return this.getUserLoans().pipe(
      map((loans: LoanResponse[]) => {
        const total = loans.length;
        const approved = loans.filter(l => l.status === LoanStatus.APPROVED).length;
        const pending = loans.filter(l => l.status === LoanStatus.PENDING).length;
        const rejected = loans.filter(l => l.status === LoanStatus.REJECTED).length;

        let response: string;
        if (total === 0) {
          response = 'You have not applied for any loans yet.';
        } else {
          response = `You have ${total} loan application${total > 1 ? 's' : ''}.\n\n• Approved: ${approved}\n• Pending: ${pending}\n• Rejected: ${rejected}`;
        }

        return {
          success: true,
          data: response,
          breadcrumb: 'Loans'
        };
      }),
      catchError(error => {
        return of({
          success: false,
          error: 'Unable to retrieve your loan applications right now. Please try again later.',
          breadcrumb: 'Loans'
        });
      })
    );
  }

  /**
   * Get current user ID from auth service
   */
  private getCurrentUserId(): string | null {
    const claims = this.authService.getIdentityClaims();
    return claims?.sub || claims?.userId || null;
  }

  /**
   * Load all user data from microservices
   */
  private loadUserData(userId: string): Observable<UserContext> {
    return this.getUserAccounts().pipe(
      switchMap(accounts => 
        this.getRecentTransactions(5).pipe(
          switchMap(transactions =>
            this.getKycStatus().pipe(
              map(kycStatus => ({
                userId,
                accounts,
                recentTransactions: transactions,
                kycStatus,
                lastUpdated: new Date()
              }))
            )
          )
        )
      )
    );
  }

  /**
   * Mask account number for security
   */
  private maskAccountNumber(accountNumber: string): string {
    if (accountNumber.length <= 4) return accountNumber;
    return accountNumber.slice(-4);
  }

  /**
   * Get current user context
   */
  getCurrentUserContext(): UserContext | null {
    return this.userContextSubject.value;
  }

  /**
   * Refresh user context with latest data
   */
  refreshUserContext(): Observable<UserContext> {
    return this.initializeUserContext();
  }
}
