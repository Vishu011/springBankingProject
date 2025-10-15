import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, forkJoin } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { ChatbotApiService, ChatbotApiResponse, UserContext } from './chatbot-api.service';
import { ChatMessage, ChatbotState, QuickAction } from './chatbot.models';
import { CardResponse } from '../../shared/models/card.model';
import { LoanResponse } from '../../shared/models/loan.model';

/**
 * ChatbotService
 * - Manages chatbot UI state and conversation history
 * - Routes user intents to ChatbotApiService
 * - Adds assistant messages back into the conversation
 */
@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  private stateSubject = new BehaviorSubject<ChatbotState>({
    isOpen: false,
    currentBreadcrumb: 'Home',
    conversationHistory: [],
    userContext: {}
  });

  public state$ = this.stateSubject.asObservable();

  constructor(private api: ChatbotApiService) {}

  toggleChatbot(): void {
    const state = this.stateSubject.value;
    this.setState({ isOpen: !state.isOpen });
  }

  refreshUserContext(): Observable<UserContext> {
    // Merge base context (accounts, transactions, kyc) with cards + loans
    return forkJoin({
      base: this.api.refreshUserContext(),
      creditCards: this.api.getUserCreditCards().pipe(catchError(() => of([] as CardResponse[]))),
      loans: this.api.getUserLoans().pipe(catchError(() => of([] as LoanResponse[])))
    }).pipe(
      tap(({ base, creditCards, loans }) => {
        const state = this.stateSubject.value;
        this.setState({
          userContext: {
            ...state.userContext,
            userId: base.userId,
            accounts: base.accounts,
            kycStatus: base.kycStatus,
            creditCards,
            loans
          }
        });
      }),
      map(({ base }) => base)
    );
  }

  sendMessage(text: string): void {
    const trimmed = (text || '').trim();
    if (!trimmed) return;

    const userMsg: ChatMessage = {
      id: this.genId(),
      type: 'user',
      content: trimmed,
      timestamp: new Date()
    };
    this.appendMessage(userMsg);

    // Detect and handle intent
    const handler$ = this.handleIntent(trimmed);

    handler$.subscribe({
      next: (resp: ChatbotApiResponse) => {
        if (resp.success) {
          const assistant: ChatMessage = {
            id: this.genId(),
            type: 'assistant',
            content: typeof resp.data === 'string' ? resp.data : JSON.stringify(resp.data),
            timestamp: new Date(),
            breadcrumb: resp.breadcrumb || this.stateSubject.value.currentBreadcrumb,
            quickActions: this.defaultQuickActionsFor(resp.breadcrumb || '')
          };
          this.appendMessage(assistant, resp.breadcrumb);
        } else {
          this.appendMessage({
            id: this.genId(),
            type: 'assistant',
            content: resp.error || 'Sorry, I could not process that request.',
            timestamp: new Date(),
            breadcrumb: resp.breadcrumb || 'Home'
          });
        }
      },
      error: () => {
        this.appendMessage({
          id: this.genId(),
          type: 'assistant',
          content: 'Something went wrong. Please try again later.',
          timestamp: new Date(),
          breadcrumb: 'Home'
        });
      }
    });
  }

  // Intent detection and routing
  private handleIntent(query: string): Observable<ChatbotApiResponse> {
    const q = query.toLowerCase();

    // Menu/help
    if (/(^|\s)(menu|help|options)(\s|$)/.test(q)) {
      return of(this.menuResponse());
    }

    // Cards related
    if (/(credit\s*card|debit\s*card|cards?\b)/.test(q)) {
      return this.api.processCreditCardSummaryQuery();
    }

    // Loans related
    if (/\bloan(s)?\b/.test(q) || /(loan\s*status|loan\s*applications?)/.test(q)) {
      return this.api.processLoanSummaryQuery();
    }

    // Accounts balance
    if (/(balance|account\s*balance|my\s*balance)/.test(q)) {
      return this.api.processAccountBalanceQuery();
    }

    // Accounts info
    if (/\baccount(s)?\b/.test(q)) {
      return this.api.processAccountInfoQuery();
    }

    // Transactions / statements
    if (/(transaction(s)?|statement(s)?|history|recent)/.test(q)) {
      return this.api.processTransactionHistoryQuery();
    }

    // KYC
    if (/\bkyc\b/.test(q) || /verify|verification/.test(q)) {
      return this.api.processKycStatusQuery();
    }

    // Default -> show menu
    return of({
      success: true,
      data: this.unknownResponse(),
      breadcrumb: 'Home'
    });
  }

  private menuResponse(): ChatbotApiResponse {
    const content =
      'Here are some things I can help you with:\n\n' +
      '• Show my account balance\n' +
      '• Show my accounts\n' +
      '• Show my recent transactions\n' +
      '• Show my KYC status\n' +
      '• Show my cards\n' +
      '• Show my loans status\n';
    return { success: true, data: content, breadcrumb: 'Home' };
  }

  private unknownResponse(): string {
    return (
      'I did not understand that. Try one of the following:\n\n' +
      '• Show my account balance\n' +
      '• Show my accounts\n' +
      '• Show my recent transactions\n' +
      '• Show my KYC status\n' +
      '• Show my cards\n' +
      '• Show my loans status\n'
    );
  }

  private defaultQuickActionsFor(breadcrumb: string): QuickAction[] {
    const common: QuickAction[] = [
      { label: 'Show menu', type: 'message', payload: 'Show menu' },
      { label: 'Recent transactions', type: 'message', payload: 'Show my recent transactions' }
    ];

    if (/Cards/i.test(breadcrumb)) {
      return [
        { label: 'Show my cards', type: 'message', payload: 'Show my cards' },
        ...common
      ];
    }

    if (/Loans/i.test(breadcrumb)) {
      return [
        { label: 'Show my loans status', type: 'message', payload: 'Show my loans status' },
        ...common
      ];
    }

    if (/Accounts|Statements|KYC/i.test(breadcrumb)) {
      return [
        { label: 'Show my account balance', type: 'message', payload: 'Show my account balance' },
        ...common
      ];
    }

    return common;
    }

  // State helpers
  private setState(patch: Partial<ChatbotState>): void {
    this.stateSubject.next({
      ...this.stateSubject.value,
      ...patch
    });
  }

  private appendMessage(message: ChatMessage, breadcrumb?: string): void {
    const state = this.stateSubject.value;
    this.stateSubject.next({
      ...state,
      currentBreadcrumb: breadcrumb || state.currentBreadcrumb,
      conversationHistory: [...state.conversationHistory, message]
    });
  }

  private genId(): string {
    // Lightweight unique id for messages
    return 'msg_' + Math.random().toString(36).slice(2) + Date.now().toString(36);
  }
}
