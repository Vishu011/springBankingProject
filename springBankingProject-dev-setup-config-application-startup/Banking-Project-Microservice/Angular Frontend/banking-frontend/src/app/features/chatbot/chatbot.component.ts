import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatbotService } from './chatbot.service';
import type { ChatMessage, ChatbotState, QuickAction } from './chatbot.models';
import { Subject, takeUntil } from 'rxjs';
import { Router } from '@angular/router';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer', { static: false }) messagesContainer!: ElementRef;
  
  private destroy$ = new Subject<void>();
  
  state: ChatbotState = {
    isOpen: false,
    currentBreadcrumb: 'Home',
    conversationHistory: [],
    userContext: {}
  };
  
  userInput = '';
  isTyping = false;
  isLoading = false;
  isRefreshing = false;
  private lastActionMessageId: string | null = null;

  constructor(private chatbotService: ChatbotService, private router: Router) {}

  ngOnInit(): void {
    this.chatbotService.state$
      .pipe(takeUntil(this.destroy$))
      .subscribe((state: ChatbotState) => {
        this.state = state;
        // Stop typing indicator when assistant responds
        const last = state.conversationHistory[state.conversationHistory.length - 1];
        if (last && last.type === 'assistant') {
          this.isTyping = false;
        }
        this.tryExecutePendingAction(state);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  toggleChatbot(): void {
    this.chatbotService.toggleChatbot();
  }

  sendMessage(message?: string): void {
    const messageToSend = message || this.userInput.trim();
    if (messageToSend && !this.isLoading) {
      this.isLoading = true;
      this.isTyping = true;
      this.chatbotService.sendMessage(messageToSend);
      this.userInput = '';
      
      // Reset loading state after a delay
      setTimeout(() => {
        this.isLoading = false;
      }, 2000);
    }
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const element = this.messagesContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  formatMessage(content: string): string {
    // Convert markdown-like formatting to HTML
    return content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/\n/g, '<br>')
      .replace(/•/g, '&bull;');
  }

  getMessageClass(message: ChatMessage): string {
    return message.type === 'user' ? 'user-message' : 'assistant-message';
  }

  formatTimestamp(timestamp: Date): string {
    return timestamp.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: true 
    });
  }

  refreshContext(): void {
    if (this.isRefreshing) return;
    this.isRefreshing = true;
    this.isTyping = true;

    this.chatbotService.refreshUserContext().subscribe({
      next: () => {},
      error: () => {
        this.isRefreshing = false;
        this.isTyping = false;
      },
      complete: () => {
        this.isRefreshing = false;
        this.isTyping = false;
      }
    });
  }

  handleQuickAction(action: QuickAction): void {
    if (action.type === 'navigate' && action.path) {
      this.router.navigateByUrl(action.path);
    } else if (action.type === 'message' && action.payload) {
      this.sendMessage(action.payload);
    }
  }

  private tryExecutePendingAction(state: ChatbotState): void {
    if (!state.conversationHistory.length) return;
    const last = state.conversationHistory[state.conversationHistory.length - 1];
    if (last.type === 'assistant' && last.action && last.id !== this.lastActionMessageId) {
      this.lastActionMessageId = last.id;
      if (last.action.type === 'navigate' && last.action.path) {
        this.router.navigateByUrl(last.action.path);
      }
    }
  }
}
