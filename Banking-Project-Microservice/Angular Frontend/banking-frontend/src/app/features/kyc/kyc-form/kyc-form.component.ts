import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { KycService, KycSubmitPayload } from '../kyc.service';

@Component({
  selector: 'app-kyc-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './kyc-form.component.html',
  styleUrls: ['./kyc-form.component.css']
})
export class KycFormComponent {
  // Patterns based on requirements
  private readonly AADHAR_PATTERN = /^\d{12}$/;
  private readonly PAN_PATTERN = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
  private readonly POSTAL_PATTERN = /^\d{6}$/;

  submitting = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  selectedFiles: File[] = [];
  readonly maxFileSizeBytes = 10 * 1024 * 1024; // 10 MB per file
  readonly allowedMimeTypes = ['image/jpeg', 'image/png', 'application/pdf'];

  form = this.fb.group({
    aadharNumber: ['', [Validators.required, Validators.pattern(this.AADHAR_PATTERN)]],
    panNumber: ['', [Validators.required, Validators.pattern(this.PAN_PATTERN)]],
    addressLine1: ['', [Validators.required, Validators.minLength(3)]],
    addressLine2: [''],
    city: ['', [Validators.required, Validators.minLength(2)]],
    state: ['', [Validators.required, Validators.minLength(2)]],
    postalCode: ['', [Validators.required, Validators.pattern(this.POSTAL_PATTERN)]],
  });

  constructor(
    private fb: FormBuilder,
    private kycService: KycService,
    private router: Router
  ) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }
    const newFiles = Array.from(input.files);

    const rejected: string[] = [];
    for (const f of newFiles) {
      if (!this.allowedMimeTypes.includes(f.type)) {
        rejected.push(`${f.name} (type not allowed)`);
        continue;
      }
      if (f.size > this.maxFileSizeBytes) {
        rejected.push(`${f.name} (exceeds ${Math.round(this.maxFileSizeBytes / (1024 * 1024))}MB)`);
        continue;
      }
      this.selectedFiles.push(f);
    }

    if (rejected.length) {
      this.errorMessage = `Some files were rejected: ${rejected.join(', ')}`;
      setTimeout(() => (this.errorMessage = null), 5000);
    }

    // Clear input value so same file can be selected again if removed
    input.value = '';
  }

  removeFile(idx: number): void {
    this.selectedFiles.splice(idx, 1);
  }

  onPanInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const raw = input?.value ?? '';
    this.panNumber?.setValue(raw.toUpperCase(), { emitEvent: false });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    this.successMessage = null;
    this.submitting = true;

    const value = this.form.value;
    const payload: KycSubmitPayload = {
      aadharNumber: value.aadharNumber!,
      panNumber: value.panNumber!,
      addressLine1: value.addressLine1!,
      addressLine2: value.addressLine2 || undefined,
      city: value.city!,
      state: value.state!,
      postalCode: value.postalCode!,
      documents: this.selectedFiles.length ? this.selectedFiles : undefined
    };

    this.kycService.submitApplication(payload).subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage = 'KYC application submitted successfully. You will be notified once it is reviewed.';
        // Navigate back to dashboard after a short delay
        setTimeout(() => this.router.navigate(['/dashboard']), 2000);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Failed to submit KYC application. Please try again.';
      }
    });
  }

  // Convenience getters
  get aadharNumber() { return this.form.get('aadharNumber'); }
  get panNumber() { return this.form.get('panNumber'); }
  get addressLine1() { return this.form.get('addressLine1'); }
  get addressLine2() { return this.form.get('addressLine2'); }
  get city() { return this.form.get('city'); }
  get state() { return this.form.get('state'); }
  get postalCode() { return this.form.get('postalCode'); }
}
