import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { ApiClientService } from "../../services/api-client.service";

@Component({
  selector: "app-loans",
  templateUrl: "./loans.component.html",
  styleUrls: ["./loans.component.css"]
})
export class LoansComponent {
  // Forms
  customerForm: FormGroup;
  loanForm: FormGroup;
  emiForm: FormGroup;

  // Data
  loans: any[] = [];
  schedule: any[] = [];
  summary: any | null = null;

  // UI State
  busy = false;
  errorMsg: string | null = null;

  constructor(private api: ApiClientService, fb: FormBuilder) {
    this.customerForm = fb.group({
      customerId: [null, [Validators.required, Validators.min(1)]]
    });

    this.loanForm = fb.group({
      loanAccountNumber: ["", [Validators.required]]
    });

    this.emiForm = fb.group({
      transactionId: [LoansComponent.guid(), [Validators.required]],
      amount: [100, [Validators.required, Validators.min(0.01)]]
    });
  }

  static guid(): string {
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
      const r = (Math.random() * 16) | 0, v = c == "x" ? r : (r & 0x3 | 0x8);
      return v.toString(16);
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

  listLoans() {
    if (this.customerForm.invalid) return;
    this.clearError();
    this.busy = true;
    const customerId = Number(this.customerForm.value.customerId);
    this.api.listLoansByCustomer(customerId).subscribe({
      next: (arr) => {
        this.loans = Array.isArray(arr) ? arr : [];
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  readSchedule() {
    if (this.loanForm.invalid) return;
    this.clearError();
    this.busy = true;
    const acc = this.loanForm.value.loanAccountNumber;
    this.api.getLoanSchedule(acc).subscribe({
      next: (list) => {
        this.schedule = Array.isArray(list) ? list : [];
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  readSummary() {
    if (this.loanForm.invalid) return;
    this.clearError();
    this.busy = true;
    const acc = this.loanForm.value.loanAccountNumber;
    this.api.getLoanSummary(acc).subscribe({
      next: (sum) => {
        this.summary = sum;
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  applyEmi() {
    if (this.loanForm.invalid || this.emiForm.invalid) return;
    this.clearError();
    this.busy = true;
    const acc = this.loanForm.value.loanAccountNumber;
    const { transactionId, amount } = this.emiForm.value;
    this.api.applyEmiDev(acc, { transactionId, amount: Number(amount) }).subscribe({
      next: () => {
        // refresh summary & schedule after apply
        this.readSummary();
        this.readSchedule();
        // rotate tx id for next operation
        this.emiForm.patchValue({ transactionId: LoansComponent.guid() });
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  pickLoan(acc: string) {
    this.loanForm.patchValue({ loanAccountNumber: acc });
    this.readSummary();
    this.readSchedule();
  }
}
