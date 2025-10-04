import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { ApiClientService } from "../../services/api-client.service";

@Component({
  selector: "app-cards",
  templateUrl: "./cards.component.html",
  styleUrls: ["./cards.component.css"]
})
export class CardsComponent {
  // Issuance flow
  issuanceForm: FormGroup;
  issuanceAppId: string | null = null;
  issuanceStatus: string | null = null;
  issuanceView: any = null;

  // Management (dev-open helpers)
  createDevForm: FormGroup;
  manageForm: FormGroup;

  // Results
  createdCard: any = null;
  fetchedCard: any = null;
  customerCards: any[] = [];

  // UI state
  busy = false;
  errorMsg: string | null = null;

  constructor(private api: ApiClientService, fb: FormBuilder) {
    this.issuanceForm = fb.group({
      customerId: [null, [Validators.required, Validators.min(1)]],
      productType: ["CREDIT_CARD"]
    });

    this.createDevForm = fb.group({
      customerId: [null, [Validators.required, Validators.min(1)]],
      productType: ["CREDIT_CARD"],
      initialLimit: [5000, [Validators.min(1)]]
    });

    this.manageForm = fb.group({
      cardId: [""],
      limit: [9000, [Validators.min(1)]],
      status: ["BLOCK"]
    });
  }

  clearError() {
    this.errorMsg = null;
  }

  private handleError(e: any) {
    this.busy = false;
    try {
      // Standard error envelope { message, correlationId, ... }
      if (e?.error?.message) {
        const cid = e?.error?.correlationId ? ` | cid=${e.error.correlationId}` : "";
        this.errorMsg = `${e.error.message}${cid}`;
        return;
      }
    } catch {}
    this.errorMsg = (e?.message || "Operation failed");
  }

  // ===== Issuance Flow =====

  submitIssuance() {
    this.clearError();
    if (this.issuanceForm.invalid) return;
    this.busy = true;
    const { customerId, productType } = this.issuanceForm.value;
    this.api.submitCardIssuance({ customerId, productType }).subscribe({
      next: (res) => {
        this.issuanceAppId = res.applicationId;
        this.issuanceStatus = res.status;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  runEligibility() {
    if (!this.issuanceAppId) return;
    this.clearError();
    this.busy = true;
    this.api.runEligibility(this.issuanceAppId).subscribe({
      next: (view) => {
        this.issuanceView = view;
        this.issuanceStatus = view?.status || this.issuanceStatus;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  approveIssuance() {
    if (!this.issuanceAppId) return;
    this.clearError();
    this.busy = true;
    this.api.approveIssuance(this.issuanceAppId).subscribe({
      next: (view) => {
        this.issuanceView = view;
        this.issuanceStatus = view?.status || "APPROVED";
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  getCardByIssuanceApp() {
    if (!this.issuanceAppId) return;
    this.clearError();
    this.busy = true;
    this.api.getCardByIssuanceApp(this.issuanceAppId).subscribe({
      next: (card) => {
        this.fetchedCard = card;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  // ===== Management Dev Helpers =====

  createCardDev() {
    this.clearError();
    if (this.createDevForm.invalid) return;
    this.busy = true;
    const { customerId, productType, initialLimit } = this.createDevForm.value;
    this.api.createCardDev({ customerId, productType, initialLimit }).subscribe({
      next: (res) => {
        this.createdCard = res;
        this.manageForm.patchValue({ cardId: res.cardId });
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  getCard() {
    const cardId = this.manageForm.value.cardId;
    if (!cardId) return;
    this.clearError();
    this.busy = true;
    this.api.getCard(cardId).subscribe({
      next: (card) => {
        this.fetchedCard = card;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  activateCard() {
    const cardId = this.manageForm.value.cardId;
    if (!cardId) return;
    this.clearError();
    this.busy = true;
    this.api.activateCard(cardId).subscribe({
      next: (card) => {
        this.fetchedCard = card;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  setLimits() {
    const cardId = this.manageForm.value.cardId;
    const limit = Number(this.manageForm.value.limit || 0);
    if (!cardId || limit <= 0) return;
    this.clearError();
    this.busy = true;
    this.api.setCardLimits(cardId, limit).subscribe({
      next: (card) => {
        this.fetchedCard = card;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  updateStatus() {
    const cardId = this.manageForm.value.cardId;
    const status = this.manageForm.value.status;
    if (!cardId || !status) return;
    this.clearError();
    this.busy = true;
    this.api.updateCardStatus(cardId, status).subscribe({
      next: (card) => {
        this.fetchedCard = card;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  listByCustomer() {
    const cid = this.createDevForm.value.customerId;
    if (!cid) return;
    this.clearError();
    this.busy = true;
    this.api.listCardsByCustomer(Number(cid)).subscribe({
      next: (cards) => {
        this.customerCards = cards || [];
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }
}
