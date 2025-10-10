// src/app/features/accounts/salary-application/salary-application.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OtpService } from '../../otp/otp.service';
import { SalaryApplicationService } from './salary-application.service';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-salary-application',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="container">
    <h2>Apply for Salary/Corporate Account</h2>

    <div *ngIf="successMessage" class="msg success">{{ successMessage }}</div>
    <div *ngIf="errorMessage" class="msg error">{{ errorMessage }}</div>

    <form (ngSubmit)="submitApplication()" #formRef="ngForm">
      <div class="form-group">
        <label for="corporateEmail">Corporate Email</label>
        <input id="corporateEmail" name="corporateEmail" type="email"
               [(ngModel)]="corporateEmail" required placeholder="name@company.com">
      </div>

      <div class="form-group">
        <label>Upload Documents</label>

        <div class="dropzone"
             [class.hover]="isDropHover"
             (dragover)="onDragOver($event)"
             (dragleave)="onDragLeave($event)"
             (drop)="onDrop($event)">
          <div>Drag & drop files here</div>
          <div class="muted">or</div>
          <label class="file-btn">
            Choose files
            <input type="file" multiple (change)="onFileChange($event)" hidden>
          </label>
        </div>

        <div class="selected" *ngIf="selectedFiles.length">
          <div class="header">
            <strong>{{ selectedFiles.length }}</strong> file(s) selected
            <button type="button" class="link" (click)="clearFiles()">Clear</button>
          </div>
          <ul>
            <li *ngFor="let f of selectedFiles; let i = index">
              <span class="name">{{ f.name }}</span>
              <span class="size">{{ formatSize(f.size) }}</span>
              <button type="button" class="small" (click)="removeFile(i)">Remove</button>
            </li>
          </ul>
        </div>

        <div class="muted small">
          Supported: images, PDF, text. Max size depends on server configuration.
        </div>
      </div>

      <div class="form-group">
        <label for="documents">Or provide Document URLs (comma separated)</label>
        <textarea id="documents" name="documents" rows="3"
                  [(ngModel)]="documentsText"
                  placeholder="https://.../offer-letter.pdf, https://.../id-card.jpg"></textarea>
        <div class="muted small">If files are selected above, they will be uploaded and these URLs will be ignored.</div>
      </div>

      <div class="form-group otp-row">
        <label for="otpCode">OTP sent to corporate email</label>
        <div class="otp-controls">
          <input id="otpCode" name="otpCode" type="text" maxlength="6"
                 [(ngModel)]="otpCode" required placeholder="Enter 6-digit OTP">
          <button type="button" (click)="sendOtp()" [disabled]="sendingOtp || !corporateEmail">
            {{ sendingOtp ? 'Sending...' : 'Send OTP' }}
          </button>
        </div>
        <small>OTP is sent using public verification flow for corporate emails.</small>
      </div>

      <div class="actions">
        <button type="submit" [disabled]="submitting || !formRef.form.valid">
          {{ submitting ? 'Submitting...' : 'Submit Application' }}
        </button>
        <button type="button" class="secondary" (click)="goToMyApplications()">
          View My Applications
        </button>
      </div>
    </form>
  </div>
  `,
  styles: [`
    .container { max-width: 760px; margin: 1rem auto; padding: 1rem; background: #fff; border-radius: 8px; }
    h2 { margin-bottom: 1rem; }
    .form-group { margin-bottom: 1rem; display: flex; flex-direction: column; }
    .otp-row .otp-controls { display: flex; gap: .5rem; align-items: center; }
    input, textarea, button { font-size: 14px; }
    input, textarea { padding: .5rem; border: 1px solid #ccc; border-radius: 4px; }
    .actions { display: flex; gap: .5rem; }
    button { padding: .5rem .9rem; border: none; border-radius: 4px; background: #1976d2; color: white; cursor: pointer; }
    button.secondary { background: #666; }
    button[disabled] { opacity: .6; cursor: not-allowed; }
    .msg { padding: .5rem .75rem; border-radius: 4px; margin-bottom: .75rem; }
    .msg.success { background: #e6f4ea; color: #167c2b; border: 1px solid #b7e2c0; }
    .msg.error { background: #fdecea; color: #a12622; border: 1px solid #f5c6cb; }
    .muted { color: #666; }
    .small { font-size: 12px; }

    .dropzone {
      border: 2px dashed #aaa; border-radius: 8px; padding: 1rem; text-align: center; background: #fafafa;
    }
    .dropzone.hover { border-color: #1976d2; background: #f0f7ff; }
    .file-btn {
      display: inline-block; margin-top: .5rem; background: #1976d2; color: #fff;
      padding: .4rem .8rem; border-radius: 4px; cursor: pointer;
    }

    .selected { margin-top: .5rem; }
    .selected .header { display: flex; align-items: center; gap: .5rem; }
    .selected .header .link { background: transparent; color: #1976d2; border: none; cursor: pointer; padding: 0; }
    .selected ul { list-style: none; padding: 0; margin: .25rem 0 0; }
    .selected li { display: flex; align-items: center; gap: .5rem; padding: .25rem 0; }
    .selected .name { flex: 1; }
    .selected .size { color: #666; font-size: 12px; }
    .selected .small { padding: .15rem .4rem; font-size: 12px; background: #eee; color: #333; border-radius: 3px; border: 1px solid #ddd; }
  `]
})
export class SalaryApplicationComponent {
  corporateEmail = '';
  documentsText = '';
  otpCode = '';

  sendingOtp = false;
  submitting = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  selectedFiles: File[] = [];
  isDropHover = false;

  constructor(
    private otpService: OtpService,
    private appService: SalaryApplicationService,
    private authService: AuthService,
    private router: Router
  ) {}

  onDragOver(evt: DragEvent): void {
    evt.preventDefault();
    evt.stopPropagation();
    this.isDropHover = true;
  }

  onDragLeave(evt: DragEvent): void {
    evt.preventDefault();
    evt.stopPropagation();
    this.isDropHover = false;
  }

  onDrop(evt: DragEvent): void {
    evt.preventDefault();
    evt.stopPropagation();
    this.isDropHover = false;
    if (!evt.dataTransfer?.files?.length) return;
    this.addFiles(evt.dataTransfer.files);
  }

  onFileChange(evt: Event): void {
    const input = evt.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.addFiles(input.files);
    // reset input to allow re-selection of the same file if needed
    input.value = '';
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  clearFiles(): void {
    this.selectedFiles = [];
  }

  private addFiles(files: FileList): void {
    for (let i = 0; i < files.length; i++) {
      const f = files.item(i);
      if (!f) continue;
      // Optional: basic client-side filtering could be added here
      this.selectedFiles.push(f);
    }
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    const kb = bytes / 1024;
    if (kb < 1024) return `${kb.toFixed(1)} KB`;
    const mb = kb / 1024;
    return `${mb.toFixed(2)} MB`;
  }

  sendOtp(): void {
    this.clearMessages();
    if (!this.corporateEmail) {
      this.errorMessage = 'Enter a corporate email to send OTP.';
      return;
    }
    this.sendingOtp = true;
    this.otpService.generatePublic({
      userId: this.corporateEmail,
      purpose: 'CONTACT_VERIFICATION',
      channels: ['EMAIL'],
      contextId: null
    }).subscribe({
      next: () => {
        this.successMessage = 'OTP sent to the provided corporate email.';
        this.sendingOtp = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to send OTP.';
        this.sendingOtp = false;
      }
    });
  }

  submitApplication(): void {
    this.clearMessages();
    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'Not authenticated.';
      return;
    }
    if (!this.corporateEmail || !this.otpCode) {
      this.errorMessage = 'Corporate email and OTP are required.';
      return;
    }

    this.submitting = true;

    if (this.selectedFiles.length > 0) {
      // Multipart flow with files
      this.appService.submitApplicationMultipart(userId, this.corporateEmail, this.otpCode, this.selectedFiles)
        .subscribe({
          next: (resp) => {
            this.successMessage = 'Application submitted successfully. Status: ' + resp.status;
            this.submitting = false;
            this.otpCode = '';
            this.clearFiles();
            // Keep URLs text intact for next time; it's ignored when files are sent
          },
          error: (err) => {
            this.errorMessage = err?.error?.message || 'Failed to submit application.';
            this.submitting = false;
          }
        });
    } else {
      // JSON fallback with URLs
      const documents = this.parseDocuments(this.documentsText);
      this.appService.submitApplication({
        userId,
        corporateEmail: this.corporateEmail,
        otpCode: this.otpCode,
        documents
      }).subscribe({
        next: (resp) => {
          this.successMessage = 'Application submitted successfully. Status: ' + resp.status;
          this.submitting = false;
          this.otpCode = '';
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Failed to submit application.';
          this.submitting = false;
        }
      });
    }
  }

  goToMyApplications(): void {
    this.router.navigate(['/accounts/salary/applications']);
  }

  private parseDocuments(text: string): string[] {
    if (!text) return [];
    return text.split(',')
      .map(s => s.trim())
      .filter(s => !!s);
  }

  private clearMessages(): void {
    this.successMessage = null;
    this.errorMessage = null;
  }
}
