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
    <button (click)="refresh()" [disabled]="loading"><i class="fa fa-refresh"></i></button>
  </div>

  <ng-container *ngIf="!loading && applications.length === 0">
    <div class="empty">No applications found.</div>
  </ng-container>

  <div *ngIf="!loading && applications.length > 0" class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>ID</th>
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
</div>

  `,
  styles: [`
   .container {
  max-width: 1000px;
  margin: 2rem auto;
  padding: 2rem;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
  font-family: 'Inter', sans-serif;
}

h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #D50032;
  font-weight: 700;
  font-size: 1.8rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
}

button {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: none;
  background: #D50032;
  color: white;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  background: #A50034;
}

.msg {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  font-weight: 500;
  text-align: center;
}

.msg.info { background: #eef5ff; color: #0b57d0; border: 1px solid #cfe0ff; }
.msg.success { background: #e6f4ea; color: #167c2b; border: 1px solid #b7e2c0; }
.msg.error { background: #fdecea; color: #a12622; border: 1px solid #f5c6cb; }

table {
  width: 100%;
  border-collapse: collapse;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

thead {
  background: #f7fafc;
}

th, td {
  border-bottom: 1px solid #e5e7eb;
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.95rem;
  vertical-align: middle;
}

th {
  font-weight: 600;
  color: #333;
}

tbody tr:hover {
  background: #fdf2f2;
}

.badge {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.badge-submitted { background: #fff7e6; color: #a35d00; border: 1px solid #ffd699; }
.badge-approved { background: #e6f4ea; color: #167c2b; border: 1px solid #b7e2c0; }
.badge-rejected { background: #fdecea; color: #a12622; border: 1px solid #f5c6cb; }

.empty {
  padding: 1rem;
  border: 2px dashed #cbd5e1;
  color: #64748b;
  border-radius: 12px;
  text-align: center;
  font-size: 1rem;
}

@media (max-width: 900px) {
  table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }
}

@media (max-width: 600px) {
  h2 { font-size: 1.5rem; }
  th, td { padding: 0.5rem; font-size: 0.9rem; }
  button { width: 100%; }
}

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
