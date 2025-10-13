// banking-admin-dashboard/src/app/features/salary-applications/salary-application-detail/salary-application-detail.component.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminSalaryApplicationService, SalaryApplication } from '../salary-application.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-salary-application-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="container py-3" *ngIf="app; else loadingTpl">
      <div class="d-flex align-items-center mb-3">
        <a class="btn btn-link px-0 me-2" [routerLink]="['/salary-applications']">
          <i class="fas fa-arrow-left me-1"></i> Back to list
        </a>
        <h3 class="mb-0">Application Detail</h3>
      </div>

      <div class="row g-3">
        <div class="col-lg-8">
          <div class="card mb-3">
            <div class="card-header">
              <strong>Application Info</strong>
            </div>
            <div class="card-body">
              <div class="row mb-2">
                <div class="col-sm-4 text-muted">Application ID</div>
                <div class="col-sm-8">{{ app.applicationId }}</div>
              </div>
              <div class="row mb-2">
                <div class="col-sm-4 text-muted">User ID</div>
                <div class="col-sm-8">{{ app.userId }}</div>
              </div>
              <div class="row mb-2">
                <div class="col-sm-4 text-muted">Corporate Email</div>
                <div class="col-sm-8">{{ app.corporateEmail }}</div>
              </div>
              <div class="row mb-2">
                <div class="col-sm-4 text-muted">Submitted At</div>
                <div class="col-sm-8">{{ app.submittedAt | date:'medium' }}</div>
              </div>
              <div class="row mb-2">
                <div class="col-sm-4 text-muted">Status</div>
                <div class="col-sm-8">
                  <span class="badge"
                        [ngClass]="{
                          'bg-warning text-dark': app.status === 'SUBMITTED',
                          'bg-success': app.status === 'APPROVED',
                          'bg-danger': app.status === 'REJECTED'
                        }">
                    {{ app.status }}
                  </span>
                </div>
              </div>
              <div class="row mb-2">
                <div class="col-sm-4 text-muted">Reviewed At</div>
                <div class="col-sm-8">{{ app.reviewedAt ? (app.reviewedAt | date:'medium') : '-' }}</div>
              </div>
              <div class="row mb-2">
                <div class="col-sm-4 text-muted">Reviewer</div>
                <div class="col-sm-8">{{ app.reviewerId || '-' }}</div>
              </div>
              <div class="row">
                <div class="col-sm-4 text-muted">Admin Comment</div>
                <div class="col-sm-8">
                  <div *ngIf="app.adminComment; else noComment">{{ app.adminComment }}</div>
                  <ng-template #noComment>-</ng-template>
                </div>
              </div>
            </div>
          </div>

          <div class="card" *ngIf="app.documents?.length">
            <div class="card-header">
              <strong>Documents</strong>
            </div>
            <div class="card-body">
              <div class="list-group">
                <div class="list-group-item" *ngFor="let doc of app.documents; let i = index">
                  <div class="d-flex justify-content-between align-items-center">
                    <div class="me-3">
                      <i class="far fa-file-alt me-2"></i>
                      <span class="text-monospace">{{ doc }}</span>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary" type="button" (click)="togglePreview(i)">
                      {{ previewIndex === i ? 'Hide Preview' : 'View' }}
                    </button>
                  </div>
                  <div class="mt-3" *ngIf="previewIndex === i">
                    <ng-container [ngSwitch]="getDocType(doc)">
                      <img *ngSwitchCase="'image'" [src]="previewUrl!" alt="Document image" class="img-fluid rounded border" />
                      <iframe *ngSwitchCase="'pdf'" [src]="previewUrl!" class="w-100 border rounded" style="height: 500px;"></iframe>
                      <div *ngSwitchDefault class="text-muted">Preview not available. Use the button to open.</div>
                    </ng-container>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        <div class="col-lg-4">
          <div class="card">
            <div class="card-header">
              <strong>Review</strong>
            </div>
            <div class="card-body">
              <div class="mb-3">
                <label for="reviewerId" class="form-label">Reviewer ID</label>
                <input id="reviewerId" class="form-control" [(ngModel)]="reviewerId" placeholder="sub (from token)" />
                <div class="form-text">Auto-filled from token subject (editable).</div>
              </div>
              <div class="mb-3">
                <label for="adminComment" class="form-label">Admin Comment</label>
                <textarea id="adminComment" rows="4" class="form-control" [(ngModel)]="adminComment" placeholder="Optional comment..."></textarea>
              </div>
              <div class="d-grid gap-2">
                <button class="btn btn-success" (click)="approve()" [disabled]="submitting || app.status!=='SUBMITTED'">
                  <i class="fas fa-check me-1"></i> Approve
                </button>
                <button class="btn btn-danger" (click)="reject()" [disabled]="submitting || app.status!=='SUBMITTED'">
                  <i class="fas fa-times me-1"></i> Reject
                </button>
              </div>
              <div class="mt-3">
                <div *ngIf="error" class="alert alert-danger py-2">{{ error }}</div>
                <div *ngIf="success" class="alert alert-success py-2">{{ success }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ng-template #loadingTpl>
      <div class="container py-5 text-center">
        <div class="spinner-border text-primary mb-3" role="status"><span class="visually-hidden">Loading...</span></div>
        <div class="text-muted">Loading application...</div>
      </div>
    </ng-template>
  `
})
export class SalaryApplicationDetailComponent implements OnInit, OnDestroy {
  app: SalaryApplication | null = null;
  adminComment = '';
  reviewerId = '';
  submitting = false;
  error: string | null = null;
  success: string | null = null;
  previewIndex: number | null = null;
  previewUrl: string | null = null;
  previewMime: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private adminApps: AdminSalaryApplicationService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.load(id);
    // Try to get reviewerId from token "sub"
    const claims = this.auth.getIdentityClaims() as any;
    const sub = claims?.sub || claims?.preferred_username || '';
    this.reviewerId = sub;
  }

  ngOnDestroy(): void {
    this.clearPreview();
  }

  load(id: string): void {
    this.error = null;
    this.adminApps.getOne(id).subscribe({
      next: (app) => (this.app = app),
      error: (err) => {
        console.error('Failed to load application:', err);
        this.error = err?.error?.message || 'Failed to load application.';
      }
    });
  }

  approve(): void {
    if (!this.app) return;
    if (!confirm('Approve this application and create the Salary/Corporate account?')) return;
    this.submitDecision('APPROVED');
  }

  reject(): void {
    if (!this.app) return;
    if (!confirm('Reject this application?')) return;
    this.submitDecision('REJECTED');
  }

  private submitDecision(decision: 'APPROVED' | 'REJECTED'): void {
    if (!this.app) return;
    this.submitting = true;
    this.error = null;
    this.success = null;

    this.adminApps.review(this.app.applicationId, decision, this.reviewerId || undefined, this.adminComment || undefined)
      .subscribe({
        next: (updated) => {
          this.app = updated;
          this.success = `Application ${decision} successfully.`;
          this.submitting = false;
        },
        error: (err) => {
          console.error('Review failed:', err);
          this.error = err?.error?.message || 'Failed to submit review.';
          this.submitting = false;
        }
      });
  }

  togglePreview(index: number): void {
    if (!this.app) return;

    if (this.previewIndex === index) {
      this.clearPreview();
      this.previewIndex = null;
      return;
    }

    this.clearPreview();
    this.previewIndex = index;

    const path = this.app.documents[index];
    this.adminApps.downloadDocument(this.app.applicationId, path).subscribe({
      next: (blob) => {
        this.previewMime = blob.type || '';
        this.previewUrl = URL.createObjectURL(blob);
      },
      error: (err) => {
        console.error('Failed to load document:', err);
        this.previewUrl = null;
      }
    });
  }

  private clearPreview(): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
  }

  getDocType(url: string): 'image' | 'pdf' | 'other' {
    const lower = (url || '').toLowerCase();
    if (lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.gif') || lower.endsWith('.webp')) {
      return 'image';
    }
    if (lower.endsWith('.pdf')) {
      return 'pdf';
    }
    return 'other';
  }
}
