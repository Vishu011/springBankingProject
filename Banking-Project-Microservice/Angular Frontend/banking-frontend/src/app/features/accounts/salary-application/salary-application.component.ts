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
    .container {
  max-width: 800px;
  margin: 2rem auto;
  padding: 2rem;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
  font-family: 'Inter', sans-serif;
}

h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #D50032;
  font-weight: 700;
  font-size: 1.8rem;
}

form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

label {
  font-weight: 600;
  color: #333;
}

input, textarea, select, button {
  font-family: 'Inter', sans-serif;
}

input, textarea, select {
  padding: 0.75rem 1rem;
  border: 1.5px solid #ccc;
  border-radius: 8px;
  font-size: 0.95rem;
  transition: all 0.3s ease;
}

input:focus, textarea:focus, select:focus {
  border-color: #D50032;
  box-shadow: 0 0 0 4px rgba(213, 0, 50, 0.1);
  outline: none;
}

textarea {
  resize: vertical;
}

button {
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  transition: all 0.3s ease;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}

button[type="submit"] {
  background: linear-gradient(135deg, #D50032 0%, #A50034 100%);
  color: white;
  flex: 1 1 auto;
  min-width: 180px;
}

button.secondary {
  background: #666;
  color: white;
  flex: 1 1 auto;
  min-width: 180px;
}

.msg {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  font-weight: 500;
  text-align: center;
}

.msg.success {
  background-color: #e6f4ea;
  color: #167c2b;
  border: 1px solid #b7e2c0;
}

.msg.error {
  background-color: #fdecea;
  color: #a12622;
  border: 1px solid #f5c6cb;
}

.muted {
  color: #666;
}

.small {
  font-size: 12px;
}

.dropzone {
  border: 2px dashed #ccc;
  border-radius: 12px;
  padding: 1.25rem;
  text-align: center;
  background: #fafafa;
  transition: all 0.3s ease;
}

.dropzone.hover {
  border-color: #D50032;
  background: #fff0f1;
}

.file-btn {
  display: inline-block;
  margin-top: 0.5rem;
  background: #D50032;
  color: #fff;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.file-btn:hover {
  background: #A50034;
}

.selected {
  margin-top: 0.5rem;
  background: #f7f7f7;
  padding: 0.75rem;
  border-radius: 8px;
}

.selected .header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.selected ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.selected li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.25rem 0;
}

.selected .small {
  padding: 0.25rem 0.5rem;
  font-size: 12px;
  background: #eee;
  color: #333;
  border-radius: 4px;
}

.otp-row .otp-controls {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.otp-row input {
  flex: 1 1 150px;
}

.otp-row button {
  flex: 0 0 auto;
  background: #D50032;
  color: white;
}

.otp-row button:hover {
  background: #A50034;
}

/* Responsive */
@media (max-width: 600px) {
  .actions {
    flex-direction: column;
    gap: 0.75rem;
  }

  button {
    width: 100%;
  }
}

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
