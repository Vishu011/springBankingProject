// banking-admin-dashboard/src/app/features/card-applications/card-application-detail.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CardApplicationsAdminService } from './card-applications.service';
import { CardApplicationResponse, ReviewCardApplicationRequest, CardKind } from '../../shared/models/card.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-card-application-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container py-3" *ngIf="app">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0">Review Card Application</h2>
        <a class="btn btn-outline-secondary" routerLink="/card-applications">Back to list</a>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
      <div *ngIf="success" class="alert alert-success">{{ success }}</div>

      <div class="row g-3">
        <div class="col-12 col-lg-8">
          <div class="card">
            <div class="card-header">
              <strong>Application Details</strong>
            </div>
            <div class="card-body">
              <div class="row mb-2">
                <div class="col-sm-6">
                  <div><strong>Application ID:</strong> {{ app.applicationId }}</div>
                  <div><strong>User ID:</strong> {{ app.userId }}</div>
                  <div><strong>Account ID:</strong> {{ app.accountId }}</div>
                </div>
                <div class="col-sm-6">
                  <div><strong>Type:</strong> {{ app.type }}</div>
                  <div><strong>Requested Brand:</strong> {{ app.requestedBrand }}</div>
                  <div><strong>Submitted At:</strong> {{ app.submittedAt ? (app.submittedAt | date:'short') : '-' }}</div>
                </div>
              </div>

              <div class="row mb-2">
                <div class="col-sm-6">
                  <div><strong>Status:</strong> <span class="badge"
                    [ngClass]="{
                      'bg-warning text-dark': app.status === 'SUBMITTED',
                      'bg-success': app.status === 'APPROVED',
                      'bg-danger': app.status === 'REJECTED'
                    }">{{ app.status }}</span></div>
                </div>
                <div class="col-sm-6">
                  <div><strong>Reviewed At:</strong> {{ app.reviewedAt ? (app.reviewedAt | date:'short') : '-' }}</div>
                  <div><strong>Reviewer:</strong> {{ app.reviewerId || '-' }}</div>
                </div>
              </div>

              <div class="row mb-2" *ngIf="app.maskedPan">
                <div class="col-sm-6">
                  <div><strong>Masked PAN:</strong> {{ app.maskedPan }}</div>
                </div>
                <div class="col-sm-6" *ngIf="app.maskedCvv">
                  <div><strong>Masked CVV:</strong> {{ app.maskedCvv }}</div>
                </div>
              </div>

              <div class="alert alert-info" *ngIf="approvalResponseOneTimeCvv">
                <i class="fas fa-key me-1"></i>
                One-time CVV (display once): <strong>{{ approvalResponseOneTimeCvv }}</strong>
              </div>
            </div>
          </div>

          <div class="card mt-3" *ngIf="app.status === 'SUBMITTED'">
            <div class="card-header">
              <strong>Review Decision</strong>
            </div>
            <div class="card-body">
              <div class="mb-3">
                <label class="form-label">Decision</label>
                <div class="d-flex gap-3">
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="decisionApprove" name="decision" [(ngModel)]="decision" value="APPROVED">
                    <label class="form-check-label" for="decisionApprove">Approve</label>
                  </div>
                  <div class="form-check">
                    <input class="form-check-input" type="radio" id="decisionReject" name="decision" [(ngModel)]="decision" value="REJECTED">
                    <label class="form-check-label" for="decisionReject">Reject</label>
                  </div>
                </div>
              </div>

              <div class="row g-3" *ngIf="decision === 'APPROVED'">
                <div class="col-md-4" *ngIf="app.type === 'CREDIT'">
                  <label for="approvedLimit" class="form-label">Approved Credit Limit</label>
                  <input id="approvedLimit" type="number" min="1000" class="form-control" [(ngModel)]="approvedLimit" placeholder="Enter limit (₹)">
                  <div class="form-text">Required for CREDIT. Min ₹1,000.</div>
                </div>

                <div class="col-md-3">
                  <label for="expiryMonth" class="form-label">Expiry Month (optional)</label>
                  <select id="expiryMonth" class="form-select" [(ngModel)]="expiryMonth">
                    <option [ngValue]="null">Default</option>
                    <option *ngFor="let m of months" [ngValue]="m">{{ m }}</option>
                  </select>
                </div>

                <div class="col-md-3">
                  <label for="expiryYear" class="form-label">Expiry Year (optional)</label>
                  <select id="expiryYear" class="form-select" [(ngModel)]="expiryYear">
                    <option [ngValue]="null">Default</option>
                    <option *ngFor="let y of years" [ngValue]="y">{{ y }}</option>
                  </select>
                </div>
              </div>

              <div class="mt-3">
                <label for="adminComment" class="form-label">Admin Comment (optional)</label>
                <textarea id="adminComment" class="form-control" rows="3" [(ngModel)]="adminComment" placeholder="Notes for audit/communication"></textarea>
              </div>

              <div class="mt-3 d-flex gap-2">
                <button class="btn btn-success" (click)="submitReview()" [disabled]="submitting">
                  <i class="fas fa-check me-1"></i>{{ submitting ? 'Submitting...' : 'Submit Review' }}
                </button>
                <button class="btn btn-outline-secondary" (click)="goBack()" [disabled]="submitting">
                  Cancel
                </button>
              </div>

              <div *ngIf="validationError" class="text-danger mt-2">
                {{ validationError }}
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 col-lg-4">
          <div class="card">
            <div class="card-header">
              <strong>Guidelines</strong>
            </div>
            <div class="card-body small">
              <ul class="mb-0">
                <li>Default expiry is 5 years from now if not overridden.</li>
                <li>For CREDIT approval, approved limit is mandatory.</li>
                <li>Fees are deducted automatically after approval per account type.</li>
                <li>Upon approval, PAN and a one-time CVV will be generated and shown here once.</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="container py-3" *ngIf="!app && !error">
      <div class="alert alert-info">Loading application...</div>
    </div>
  `
})
export class CardApplicationDetailComponent implements OnInit {
  app: CardApplicationResponse | null = null;
  error: string | null = null;
  success: string | null = null;

  decision: 'APPROVED' | 'REJECTED' = 'APPROVED';
  approvedLimit: number | null = null;
  expiryMonth: number | null = null;
  expiryYear: number | null = null;
  adminComment: string | null = null;

  months: number[] = Array.from({ length: 12 }, (_, i) => i + 1);
  years: number[] = [];
  submitting = false;
  approvalResponseOneTimeCvv: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apps: CardApplicationsAdminService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const now = new Date().getFullYear();
    this.years = Array.from({ length: 11 }, (_, i) => now + i);

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.load(id);
    } else {
      this.error = 'Invalid route: application id missing.';
    }
  }

  load(id: string): void {
    this.error = null;
    this.apps.getOne(id).subscribe({
      next: (app) => {
        this.app = app;
      },
      error: (err) => {
        console.error('Failed to load card application:', err);
        this.error = err?.error?.message || 'Failed to load application.';
      }
    });
  }

  validate(): string | null {
    if (!this.app) return 'Application not loaded.';
    if (this.decision === 'APPROVED' && this.app.type === 'CREDIT') {
      if (this.approvedLimit == null || this.approvedLimit < 1000) {
        return 'Approved credit limit is required for CREDIT and must be at least ₹1,000.';
      }
    }
    if (this.expiryMonth != null && (this.expiryMonth < 1 || this.expiryMonth > 12)) {
      return 'Expiry month must be between 1 and 12.';
    }
    if (this.expiryYear != null && (this.expiryYear < new Date().getFullYear())) {
      return 'Expiry year cannot be in the past.';
    }
    return null;
  }

  submitReview(): void {
    this.validationError = null;
    const validation = this.validate();
    if (validation) {
      this.validationError = validation;
      return;
    }

    if (!this.app) return;
    const reviewerId = this.auth.getIdentityClaims()?.sub;
    if (!reviewerId) {
      this.error = 'Reviewer identity missing. Please re-login.';
      return;
    }

    const payload: ReviewCardApplicationRequest = {
      decision: this.decision,
      reviewerId,
      adminComment: this.adminComment || null,
      approvedLimit: this.decision === 'APPROVED' && this.app.type === 'CREDIT' ? (this.approvedLimit as number) : null,
      expiryMonth: this.decision === 'APPROVED' ? (this.expiryMonth ?? null) : null,
      expiryYear: this.decision === 'APPROVED' ? (this.expiryYear ?? null) : null
    };

    this.submitting = true;
    this.apps.review(this.app.applicationId, payload).subscribe({
      next: (res) => {
        this.submitting = false;
        this.success = `Application ${res.applicationId} ${res.status.toLowerCase()} successfully.`;
        // If approved, show masked PAN and one-time CVV if present
        if (res.status === 'APPROVED') {
          this.app = res;
          this.approvalResponseOneTimeCvv = res.oneTimeCvv || null;
        } else {
          this.app = res;
        }
      },
      error: (err) => {
        this.submitting = false;
        console.error('Review failed:', err);
        this.error = err?.error?.message || 'Failed to submit review.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/card-applications']);
  }

  validationError: string | null = null;
}
