// src/app/features/cards/card-management/card-management.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardService } from '../card.service';
import { OtpService, GenerateOtpRequest } from '../../otp/otp.service';


import { AccountService } from '../../accounts/account.service'; // To get account numbers for display
import { CardResponse, CardStatus, CardBrand } from '../../../shared/models/card.model';
import { AccountResponse } from '../../../shared/models/account.model';
import { AuthService } from '../../../core/services/auth.service';


@Component({
  selector: 'app-card-management',
  standalone: true,
  imports: [CommonModule, FormsModule], // Standalone
  templateUrl: './card-management.component.html',
  styleUrls: ['./card-management.component.css']
})
export class CardManagementComponent implements OnInit {
  userCards: CardResponse[] = [];
  loading: boolean = true;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  CardStatus = CardStatus; // For template access

  userAccounts: AccountResponse[] = []; // To map accountId to accountNumber

  // Accordion active row
  currentCardForLimitUpdate: CardResponse | null = null;

  constructor(
    private cardService: CardService,
    private authService: AuthService,
    private accountService: AccountService, // Inject AccountService
    private otpService: OtpService
  ) { }

  ngOnInit(): void {
    this.loadUserCardsAndAccounts();
  }

  loadUserCardsAndAccounts(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const userId = this.authService.getIdentityClaims()?.sub;

    if (userId) {
      // Load accounts first to map accountIds to numbers
      this.accountService.getAccountsByUserId(userId).subscribe(
        (accountsData: AccountResponse[]) => {
          this.userAccounts = accountsData || [];

          // Then load cards
          this.cardService.listMyCards(userId).subscribe(
            (cardsData: CardResponse[]) => {
              this.userCards = cardsData || [];
              this.loading = false;
              if (this.userCards.length === 0) {
                this.successMessage = 'You have no cards issued.';
              }
            },
            (error: any) => {
              console.error('Error loading user cards:', error);
              this.errorMessage = error.error?.message || 'Failed to load your cards.';
              this.loading = false;
            }
          );
        },
        (error: any) => {
          console.error('Error loading user accounts for card management:', error);
          this.errorMessage = error.error?.message || 'Failed to load associated accounts.';
          this.loading = false;
        }
      );
    } else {
      this.errorMessage = 'User ID not found. Please log in again.';
      this.loading = false;
    }
  }

  getAccountNumber(accountId: string): string {
    const account = this.userAccounts.find(acc => acc.accountId === accountId);
    return account ? account.accountNumber : 'N/A';
  }

  // OTP + Reveal PAN state
  revealedPan: { [cardId: string]: string | undefined } = {};
  revealOtp: { [cardId: string]: string } = {};
  revealing: { [cardId: string]: boolean } = {};
  genOtpLoading: { [cardId: string]: boolean } = {};
  genOtpCooldownSec: { [cardId: string]: number } = {};
  private genOtpTimers: { [cardId: string]: any } = {};

  // CVV Regeneration state
  regeneratedCvv: { [cardId: string]: string | undefined } = {};
  regenOtp: { [cardId: string]: string } = {};
  regenLoading: { [cardId: string]: boolean } = {};
  regenOtpLoading: { [cardId: string]: boolean } = {};
  regenOtpCooldownSec: { [cardId: string]: number } = {};
  private regenOtpTimers: { [cardId: string]: any } = {};

  generateRevealOtp(card: CardResponse): void {
    this.errorMessage = null;
    this.successMessage = null;

    const id = card.cardId;
    if (this.genOtpLoading[id] || (this.genOtpCooldownSec[id] && this.genOtpCooldownSec[id] > 0)) {
      return;
    }

    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User ID not found. Please log in again.';
      return;
    }

    this.genOtpLoading[id] = true;
    const req: GenerateOtpRequest = {
      userId,
      purpose: 'CARD_OPERATION',
      channels: ['EMAIL'],
      contextId: card.accountId
    };
    this.otpService.generate(req).subscribe({
      next: () => {
        this.successMessage = 'OTP sent to your registered email. Enter it and click Reveal.';
        this.genOtpLoading[id] = false;
        this.startCardOtpCooldown(id, 60);
      },
      error: (err) => {
        console.error('Failed to generate OTP for PAN reveal', err);
        this.errorMessage = err?.error?.message || 'Failed to generate OTP.';
        this.genOtpLoading[id] = false;
      }
    });
  }

  private startCardOtpCooldown(cardId: string, seconds: number = 60): void {
    this.genOtpCooldownSec[cardId] = seconds;
    if (this.genOtpTimers[cardId]) {
      clearInterval(this.genOtpTimers[cardId]);
    }
    this.genOtpTimers[cardId] = setInterval(() => {
      const remaining = (this.genOtpCooldownSec[cardId] || 0) - 1;
      this.genOtpCooldownSec[cardId] = remaining > 0 ? remaining : 0;
      if (this.genOtpCooldownSec[cardId] === 0) {
        clearInterval(this.genOtpTimers[cardId]);
        this.genOtpTimers[cardId] = null;
      }
    }, 1000);
  }

  revealPan(card: CardResponse): void {
    this.errorMessage = null;
    this.successMessage = null;
    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User ID not found. Please log in again.';
      return;
    }
    const code = this.revealOtp[card.cardId];
    if (!code || code.trim().length === 0) {
      this.errorMessage = 'Enter the OTP received on your email.';
      return;
    }
    this.revealing[card.cardId] = true;
    this.cardService.revealPan(card.cardId, { userId, otpCode: code }).subscribe({
      next: (resp) => {
        this.revealedPan[card.cardId] = resp.fullPan;
        this.successMessage = 'Card number revealed for this session.';
        this.revealOtp[card.cardId] = '';
        this.revealing[card.cardId] = false;
      },
      error: (err) => {
        console.error('Reveal PAN failed', err);
        this.errorMessage = err?.error?.message || 'Reveal failed. Check OTP and try again.';
        this.revealing[card.cardId] = false;
      }
    });
  }

  generateRegenOtp(card: CardResponse): void {
    this.errorMessage = null;
    this.successMessage = null;

    const id = card.cardId;
    if (this.regenOtpLoading[id] || (this.regenOtpCooldownSec[id] && this.regenOtpCooldownSec[id] > 0)) {
      return;
    }

    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User ID not found. Please log in again.';
      return;
    }

    this.regenOtpLoading[id] = true;
    const req: GenerateOtpRequest = {
      userId,
      purpose: 'CARD_OPERATION',
      channels: ['EMAIL'],
      contextId: card.accountId
    };
    this.otpService.generate(req).subscribe({
      next: () => {
        this.successMessage = 'OTP sent to your registered email. Enter it and click Generate CVV.';
        this.regenOtpLoading[id] = false;
        this.startRegenOtpCooldown(id, 60);
      },
      error: (err) => {
        console.error('Failed to generate OTP for CVV regenerate', err);
        this.errorMessage = err?.error?.message || 'Failed to generate OTP.';
        this.regenOtpLoading[id] = false;
      }
    });
  }

  private startRegenOtpCooldown(cardId: string, seconds: number = 60): void {
    this.regenOtpCooldownSec[cardId] = seconds;
    if (this.regenOtpTimers[cardId]) {
      clearInterval(this.regenOtpTimers[cardId]);
    }
    this.regenOtpTimers[cardId] = setInterval(() => {
      const remaining = (this.regenOtpCooldownSec[cardId] || 0) - 1;
      this.regenOtpCooldownSec[cardId] = remaining > 0 ? remaining : 0;
      if (this.regenOtpCooldownSec[cardId] === 0) {
        clearInterval(this.regenOtpTimers[cardId]);
        this.regenOtpTimers[cardId] = null;
      }
    }, 1000);
  }

  regenerateCvv(card: CardResponse): void {
    this.errorMessage = null;
    this.successMessage = null;

    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'User ID not found. Please log in again.';
      return;
    }
    const code = this.regenOtp[card.cardId];
    if (!code || code.trim().length === 0) {
      this.errorMessage = 'Enter the OTP received on your email.';
      return;
    }
    this.regenLoading[card.cardId] = true;
    this.cardService.regenerateCvv(card.cardId, { userId, otpCode: code }).subscribe({
      next: (resp) => {
        this.regeneratedCvv[card.cardId] = resp.cvv;
        this.successMessage = 'New CVV generated. Copy it now. It will not be shown again.';
        this.regenOtp[card.cardId] = '';
        this.regenLoading[card.cardId] = false;
      },
      error: (err) => {
        console.error('Regenerate CVV failed', err);
        this.errorMessage = err?.error?.message || 'CVV regeneration failed. Check OTP and try again.';
        this.regenLoading[card.cardId] = false;
      }
    });
  }

  copyToClipboard(text: string): void {
    if (!text) { return; }
    if (navigator && navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        this.successMessage = 'Copied to clipboard.';
        setTimeout(() => { if (this.successMessage === 'Copied to clipboard.') this.successMessage = null; }, 2000);
      }).catch(() => {
        this.errorMessage = 'Failed to copy.';
        setTimeout(() => { if (this.errorMessage === 'Failed to copy.') this.errorMessage = null; }, 2000);
      });
    } else {
      // Fallback
      const textarea = document.createElement('textarea');
      textarea.value = text;
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand('copy');
        this.successMessage = 'Copied to clipboard.';
        setTimeout(() => { if (this.successMessage === 'Copied to clipboard.') this.successMessage = null; }, 2000);
      } catch {
        this.errorMessage = 'Failed to copy.';
        setTimeout(() => { if (this.errorMessage === 'Failed to copy.') this.errorMessage = null; }, 2000);
      } finally {
        document.body.removeChild(textarea);
      }
    }
  }

  // Refactored from inline JS toggleAccordion
  toggleAccordion(card: CardResponse): void {
    // Find the item in the list and toggle its active state
    const index = this.userCards.findIndex(c => c.cardId === card.cardId);
    if (index !== -1) {
      // Add a property to CardResponse to track active state in UI
      // Or simply manage it via a single activeCardId
      // For simplicity, let's just toggle a class based on activeCardId
      if (this.currentCardForLimitUpdate?.cardId === card.cardId) {
        this.currentCardForLimitUpdate = null; // Collapse if already open
      } else {
        this.currentCardForLimitUpdate = card; // Expand this card
      }
    }
  }







  getCardStatusClass(status: CardStatus): string {
    switch (status) {
      case CardStatus.ACTIVE: return 'status-active';
      case CardStatus.BLOCKED: return 'status-blocked';
      default: return '';
    }
  }

  getCardBrandIconClass(brand: CardBrand): string {
    switch (brand) {
      case CardBrand.VISA: return 'fab fa-cc-visa visa';
      case CardBrand.MASTERCARD: return 'fab fa-cc-mastercard mastercard';
      case CardBrand.RUPAY: return 'fas fa-credit-card rupay'; // no direct icon
      case CardBrand.AMEX: return 'fab fa-cc-amex amex';
      case CardBrand.DISCOVERY: return 'fab fa-cc-discover discover';
      default: return 'fas fa-credit-card';
    }
  }

  getCardLogoSrc(brand: CardBrand): string {
    switch (brand) {
      case CardBrand.VISA: return 'assets/images/visa.png';
      case CardBrand.MASTERCARD: return 'assets/images/mastercard.png';
      case CardBrand.RUPAY: return 'assets/images/rupay.png';
      case CardBrand.AMEX: return 'assets/images/amex.png';
      case CardBrand.DISCOVERY: return 'assets/images/discover.png';
      default: return '';
    }
  }
}
