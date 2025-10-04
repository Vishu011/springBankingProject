import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { ApiClientService } from "../../services/api-client.service";

@Component({
  selector: "app-beneficiaries",
  templateUrl: "./beneficiaries.component.html",
  styleUrls: ["./beneficiaries.component.css"]
})
export class BeneficiariesComponent {
  createForm: FormGroup;
  verifyForm: FormGroup;

  created: any = null;
  list: any[] = [];

  busy = false;
  errorMsg: string | null = null;

  constructor(private api: ApiClientService, fb: FormBuilder) {
    this.createForm = fb.group({
      customerId: [null, [Validators.required, Validators.min(1)]],
      nickname: ["", [Validators.required]],
      accountNumber: ["", [Validators.required]],
      bankCode: ["OMNI", [Validators.required]]
    });

    this.verifyForm = fb.group({
      owningCustomerId: [null, [Validators.required, Validators.min(1)]],
      beneficiaryId: [null, [Validators.required, Validators.min(1)]],
      challengeId: [null, [Validators.required, Validators.min(1)]],
      code: ["", [Validators.required]]
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

  create() {
    if (this.createForm.invalid) return;
    this.clearError();
    this.busy = true;

    const { customerId, nickname, accountNumber, bankCode } = this.createForm.value;
    this.api.createBeneficiary(Number(customerId), { nickname, accountNumber, bankCode }).subscribe({
      next: (res) => {
        this.created = res;
        // Pre-fill verify form if response contains ids
        if (res?.beneficiaryId) this.verifyForm.patchValue({ beneficiaryId: res.beneficiaryId });
        if (res?.challengeId) this.verifyForm.patchValue({ challengeId: res.challengeId });
        if (customerId) this.verifyForm.patchValue({ owningCustomerId: Number(customerId) });
        // In dev-open, an otpDevEcho may be returned to help verify
        if (res?.otpDevEcho) this.verifyForm.patchValue({ code: res.otpDevEcho });
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }

  verify() {
    if (this.verifyForm.invalid) return;
    this.clearError();
    this.busy = true;

    const body = this.verifyForm.value;
    body.owningCustomerId = Number(body.owningCustomerId);
    body.beneficiaryId = Number(body.beneficiaryId);
    body.challengeId = Number(body.challengeId);

    this.api.verifyBeneficiaryOtp(body).subscribe({
      next: () => {
        this.busy = false;
        this.listAll();
      },
      error: (e) => this.handleError(e)
    });
  }

  listAll() {
    const customerId = Number(this.createForm.value.customerId || this.verifyForm.value.owningCustomerId);
    if (!customerId) return;
    this.clearError();
    this.busy = true;
    this.api.listBeneficiaries(customerId).subscribe({
      next: (arr) => {
        this.list = Array.isArray(arr) ? arr : [];
        this.busy = false;
      },
      error: (e) => this.handleError(e)
    });
  }
}
