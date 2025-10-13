// banking-admin-dashboard/src/app/features/salary-applications/salary-applications-list/salary-applications-list.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminSalaryApplicationService, SalaryApplication, SalaryApplicationStatus } from '../salary-application.service';

@Component({
  selector: 'app-salary-applications-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container py-3">
      <h2 class="mb-3">Salary/Corporate Account Applications</h2>

      <div class="btn-group mb-3" role="group" aria-label="Status tabs">
        <button class="btn" [class.btn-primary]="status==='SUBMITTED'" [class.btn-outline-primary]="status!=='SUBMITTED'" (click)="load('SUBMITTED')">Submitted</button>
        <button class="btn" [class.btn-success]="status==='APPROVED'" [class.btn-outline-success]="status!=='APPROVED'" (click)="load('APPROVED')">Approved</button>
        <button class="btn" [class.btn-danger]="status==='REJECTED'" [class.btn-outline-danger]="status!=='REJECTED'" (click)="load('REJECTED')">Rejected</button>
      </div>

      <div *ngIf="loading" class="alert alert-info">Loading applications...</div>
      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
      <div *ngIf="!loading && !error && applications.length === 0" class="alert alert-secondary">No applications found for status "{{ status }}".</div>

      <div class="table-responsive" *ngIf="applications.length > 0">
        <table class="table table-striped table-hover align-middle">
          <thead class="table-light">
            <tr>
              <th>Application ID</th>
              <th>User ID</th>
              <th>Corporate Email</th>
              <th>Submitted At</th>
              <th>Status</th>
              <th>Reviewed At</th>
              <th>Reviewer</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let app of applications; trackBy: trackById">
              <td>{{ app.applicationId }}</td>
              <td>{{ app.userId }}</td>
              <td>{{ app.corporateEmail }}</td>
              <td>{{ app.submittedAt | date:'short' }}</td>
              <td>
                <span class="badge"
                      [ngClass]="{
                        'bg-warning text-dark': app.status === 'SUBMITTED',
                        'bg-success': app.status === 'APPROVED',
                        'bg-danger': app.status === 'REJECTED'
                      }">
                  {{ app.status }}
                </span>
              </td>
              <td>{{ app.reviewedAt ? (app.reviewedAt | date:'short') : '-' }}</td>
              <td>{{ app.reviewerId || '-' }}</td>
              <td class="text-end">
                <a class="btn btn-sm btn-outline-secondary" [routerLink]="['/salary-applications', app.applicationId]">
                  View
                </a>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class SalaryApplicationsListComponent implements OnInit {
  applications: SalaryApplication[] = [];
  status: SalaryApplicationStatus = 'SUBMITTED';
  loading = false;
  error: string | null = null;

  constructor(private adminApps: AdminSalaryApplicationService) {}

  ngOnInit(): void {
    this.load('SUBMITTED');
  }

  load(status: SalaryApplicationStatus): void {
    this.status = status;
    this.loading = true;
    this.error = null;
    this.applications = [];
    this.adminApps.listByStatus(status).subscribe({
      next: (apps) => {
        this.applications = apps;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load salary applications:', err);
        this.error = err?.error?.message || 'Failed to load applications.';
        this.loading = false;
      }
    });
  }

  trackById(index: number, app: SalaryApplication): string {
    return app.applicationId;
  }
}
