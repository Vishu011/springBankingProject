import { AccountResponse } from '../../shared/models/account.model';
import { CardResponse } from '../../shared/models/card.model';
import { LoanResponse } from '../../shared/models/loan.model';

export interface QuickAction {
  label: string;
  type: 'navigate' | 'message';
  payload?: string; // for type: 'message'
  path?: string; // for type: 'navigate'
}

export interface ChatAction {
  type: 'navigate';
  path: string;
}

export interface ChatMessage {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  breadcrumb?: string;
  quickActions?: QuickAction[];
  action?: ChatAction;
}

export interface AssistantResponse {
  content: string;
  breadcrumb?: string;
  quickActions?: QuickAction[];
  action?: ChatAction;
}

export interface ChatbotState {
  isOpen: boolean;
  currentBreadcrumb: string;
  conversationHistory: ChatMessage[];
  userContext: {
    userId?: string;
    accounts?: AccountResponse[];
    creditCards?: CardResponse[];
    loans?: LoanResponse[];
    kycStatus?: 'PENDING' | 'VERIFIED' | 'REJECTED';
  };
}
