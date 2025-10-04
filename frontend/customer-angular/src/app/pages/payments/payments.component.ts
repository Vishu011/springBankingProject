import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { ApiClientService } from "../../services/api-client.service";

@Component({
  selector: "app-payments",
  templateUrl: "./payments.component.html",
  styleUrls: ["./payments.component.css"]
})
export class PaymentsComponent {
  transferForm: FormGroup;
  paymentId: string | null = null;
  status: string | null = null;

  fromBalance: number | null = null;
  toBalance: number | null = null;

  busy = false;
  errorMsg: string | null = null;

  constructor(private api: ApiClientService, fb: FormBuilder) {
    this.transferForm = fb.group({
      customerId: [null, [Validators.required, Validators.min(1)]],
      fromAccount: ["", [Validators.required]],
      toAccount: ["", [Validators.required]],
      amount: [100, [Validators.required, Validators.min(0.01)]],
      currency: ["USD", [Validators.required]]
    });
  }

  clearError() {
    this.errorMsg = null;
  }

  private handleError(e: any) {
    this.busy = false;
    try {
      if (e?.error?.message) {
        const cid = e?.error?.correlationId ? ` | cid=${e.error.correlationId}` : "";
        this.errorMsg = `${e.error.message}${cid}`;
        return;
      }
    } catch {}
    this.errorMsg = (e?.message || "Operation failed");
  }

  initiate() {
    if (this.transferForm.invalid) return;
    this.clearError();
    this.busy = true;

    const { customerId, fromAccount, toAccount, amount, currency } = this.transferForm.value;
    this.api.initiateInternalTransfer({ customerId: Number(customerId), fromAccount, toAccount, amount: Number(amount), currency }).subscribe({
      next: (res) => {
        this.paymentId = res.paymentId || null;
        this.status = res.status || null;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  pollStatus() {
    if (!this.paymentId) return;
    this.clearError();
    this.busy = true;
    this.api.getPaymentStatus(this.paymentId).subscribe({
      next: (s) => {
        this.status = typeof s === "string" ? s : (s as any)?.status || this.status;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  readBalances() {
    const { fromAccount, toAccount } = this.transferForm.value;
    if (!fromAccount || !toAccount) return;
    this.clearError();
    this.busy = true;

    let pending = 2;
    const done = () => { pending -= 1; if (pending === 0) this.busy = false; };

    this.api.getBalance(fromAccount).subscribe({
      next: (b) => { this.fromBalance = Number(b); done(); },
      error: (e) => { this.handleError(e); done(); }
    });

    this.api.getBalance(toAccount).subscribe({
      next: (b) => { this.toBalance = Number(b); done(); },
      error: (e) => { this.handleError(e); done(); }
    });
  }
}
