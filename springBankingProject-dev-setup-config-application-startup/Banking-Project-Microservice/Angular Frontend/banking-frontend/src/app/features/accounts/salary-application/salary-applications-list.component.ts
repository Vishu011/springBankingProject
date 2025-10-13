// src/app/features/accounts/salary-application/salary-applications-list.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { SalaryApplicationService } from './salary-application.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  SalaryApplicationResponse,
  SalaryApplicationStatus
} from '../../../shared/models/salary-application.model';

@Component({
  selector: 'app-salary-applications-list',
  standalone: true,
  imports: [CommonModule],
  providers: [DatePipe],
  template: `
    <div class="container">
      <h2>My Salary/Corporate Account Applications</h2>

      <div *ngIf="loading" class="msg info">Loading applications...</div>
      <div *ngIf="errorMessage" class="msg error">{{ errorMessage }}</div>
      <div *ngIf="successMessage" class="msg success">{{ successMessage }}</div>

      <div class="actions">
        <button (click)="refresh()" [disabled]="loading">Refresh</button>
      </div>

      <ng-container *ngIf="!loading && applications.length === 0">
        <div class="empty">No applications found.</div>
      </ng-container>

      <table *ngIf="!loading && applications.length > 0">
        <thead>
          <tr>
            <th>Application ID</th>
            <th>Corporate Email</th>
            <th>Status</th>
            <th>Submitted</th>
            <th>Reviewed</th>
            <th>Reviewer</th>
            <th>Admin Comment</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let app of applications">
            <td>{{ app.applicationId }}</td>
            <td>{{ app.corporateEmail }}</td>
            <td>
              <span class="badge"
                    [ngClass]="{
                      'badge-submitted': app.status === Status.SUBMITTED,
                      'badge-approved': app.status === Status.APPROVED,
                      'badge-rejected': app.status === Status.REJECTED
                    }">
                {{ app.status }}
              </span>
            </td>
            <td>{{ app.submittedAt | date: 'short' }}</td>
            <td>{{ app.reviewedAt ? (app.reviewedAt | date: 'short') : '-' }}</td>
            <td>{{ app.reviewerId || '-' }}</td>
            <td>{{ app.adminComment || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .container { max-width: 1000px; margin: 1rem auto; padding: 1rem; background: #fff; border-radius: 8px; }
    h2 { margin-bottom: 1rem; }
    .actions { margin-bottom: .75rem; }
    .msg { padding: .5rem .75rem; border-radius: 4px; margin-bottom: .75rem; }
    .msg.info { background: #eef5ff; color: #0b57d0; border: 1px solid #cfe0ff; }
    .msg.error { background: #fdecea; color: #a12622; border: 1px solid #f5c6cb; }
    .msg.success { background: #e6f4ea; color: #167c2b; border: 1px solid #b7e2c0; }
    table { width: 100%; border-collapse: collapse; }
    th, td { border: 1px solid #e5e7eb; padding: .5rem; text-align: left; }
    thead { background: #f7fafc; }
    .badge { padding: .2rem .5rem; border-radius: 999px; font-size: 12px; }
    .badge-submitted { background: #fff7e6; color: #a35d00; border: 1px solid #ffd699; }
    .badge-approved { background: #e6f4ea; color: #167c2b; border: 1px solid #b7e2c0; }
    .badge-rejected { background: #fdecea; color: #a12622; border: 1px solid #f5c6cb; }
    .empty { padding: .75rem; border: 1px dashed #cbd5e1; color: #64748b; border-radius: 6px; }
  `]
})
export class SalaryApplicationsListComponent implements OnInit {
  applications: SalaryApplicationResponse[] = [];
  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  readonly Status = SalaryApplicationStatus;

  constructor(
    private appService: SalaryApplicationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.errorMessage = null;
    this.successMessage = null;
    const userId = this.authService.getIdentityClaims()?.sub;
    if (!userId) {
      this.errorMessage = 'Not authenticated.';
      return;
    }
    this.loading = true;
    this.appService.getMyApplications(userId).subscribe({
      next: (apps) => {
        this.applications = apps || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load applications.';
        this.loading = false;
      }
    });
  }
}
