// banking-admin-dashboard/src/app/features/self-service-requests/self-service-requests-list.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SelfServiceRequestsAdminService, AdminSelfServiceRequest } from './self-service-requests.service';

@Component({
  selector: 'app-self-service-requests-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
  <div class="container">
    <h2>User Profile Requests</h2>
    <div class="toolbar">
      <label>Status</label>
      <select [(ngModel)]="status" (change)="load()" name="status">
        <option value="SUBMITTED">SUBMITTED (Pending)</option>
        <option value="APPROVED">APPROVED</option>
        <option value="REJECTED">REJECTED</option>
      </select>
      <button (click)="load()" [disabled]="loading">Refresh</button>
    </div>

    <div *ngIf="loading" class="muted">Loading...</div>
    <div *ngIf="error" class="error">{{ error }}</div>

    <table class="table" *ngIf="!loading && items.length > 0">
      <thead>
        <tr>
          <th>Request ID</th>
          <th>User ID</th>
          <th>Type</th>
          <th>Submitted</th>
          <th>Status</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let r of items">
          <td>{{ r.requestId }}</td>
          <td>{{ r.userId }}</td>
          <td>{{ r.type }}</td>
          <td>{{ r.submittedAt || '-' }}</td>
          <td>{{ r.status }}</td>
          <td>
            <a [routerLink]="['/self-service-requests', r.requestId]">View</a>
          </td>
        </tr>
      </tbody>
    </table>

    <div *ngIf="!loading && items.length === 0" class="muted">No requests found.</div>
  </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    .toolbar { display:flex; gap: .75rem; align-items:center; margin-bottom: .75rem; }
    .muted { color:#777; }
    .error { color:#b00020; margin:.5rem 0; }
    table.table { width:100%; border-collapse: collapse; }
    table.table th, table.table td { border:1px solid #eee; padding:.5rem; text-align:left; }
  `]
})
export class SelfServiceRequestsListComponent implements OnInit {
  items: AdminSelfServiceRequest[] = [];
  status: 'SUBMITTED' | 'APPROVED' | 'REJECTED' | string = 'SUBMITTED';
  loading = false;
  error: string | null = null;

  constructor(private api: SelfServiceRequestsAdminService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.api.list(this.status).subscribe({
      next: list => { this.items = list || []; this.loading = false; },
      error: err => { this.error = err?.error?.message || 'Failed to load requests'; this.loading = false; }
    });
  }
}
