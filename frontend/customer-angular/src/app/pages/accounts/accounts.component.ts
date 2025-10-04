import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { ApiClientService } from "../../services/api-client.service";

@Component({
  selector: "app-accounts",
  templateUrl: "./accounts.component.html",
  styleUrls: ["./accounts.component.css"]
})
export class AccountsComponent {
  form: FormGroup;

  balance: number | null = null;
  history: any[] = [];

  busy = false;
  errorMsg: string | null = null;

  constructor(private api: ApiClientService, fb: FormBuilder) {
    this.form = fb.group({
      accountNumber: ["", [Validators.required]],
      historySize: [50, [Validators.min(1)]]
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

  load() {
    if (this.form.invalid) return;
    this.clearError();
    this.busy = true;
    this.balance = null;
    this.history = [];

    const account = this.form.value.accountNumber as string;
    const size = Number(this.form.value.historySize || 50);

    let pending = 2;
    const done = () => { pending -= 1; if (pending === 0) this.busy = false; };

    // Balance
    this.api.getBalance(account).subscribe({
      next: (b) => { this.balance = Number(b); done(); },
      error: (e) => { this.handleError(e); done(); }
    });

    // Ledger history
    this.api.getLedgerHistory(account, size).subscribe({
      next: (list) => {
        this.history = Array.isArray(list) ? list : [];
        done();
      },
      error: (e) => { this.handleError(e); done(); }
    });
  }
}
