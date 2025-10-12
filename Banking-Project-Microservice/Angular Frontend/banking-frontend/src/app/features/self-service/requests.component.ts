// src/app/features/self-service/requests.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { SelfServiceService } from './self-service.service';
import { AuthService } from '../../core/services/auth.service';
import {
  SelfServiceRequestDto,
  SelfServiceRequestType
} from '../../shared/models/self-service.model';

@Component({
  selector: 'app-self-service-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="container">
    <h2>Self Service Requests</h2>
    <div *ngIf="loading" class="loading">Loading...</div>
    <div *ngIf="errorMessage" class="error">{{ errorMessage }}</div>
    <div *ngIf="successMessage" class="success">{{ successMessage }}</div>

    <!-- Submit new request -->
    <div class="card">
      <h3>Submit New Request</h3>
      <form (ngSubmit)="submit()" #reqForm="ngForm">
        <div class="row">
          <label>Request Type</label>
          <select [(ngModel)]="type" name="type" required>
            <option *ngFor="let t of types" [value]="t">{{ t }}</option>
          </select>
        </div>

        <!-- Structured inputs instead of free-form JSON -->
        <ng-container [ngSwitch]="type">
          <div *ngSwitchCase="'NAME_CHANGE'">
            <div class="row">
              <label>First Name</label>
              <input type="text" [(ngModel)]="nameFirst" name="nameFirst" required />
            </div>
            <div class="row">
              <label>Middle Name (optional)</label>
              <input type="text" [(ngModel)]="nameMiddle" name="nameMiddle" />
            </div>
            <div class="row">
              <label>Last Name</label>
              <input type="text" [(ngModel)]="nameLast" name="nameLast" required />
            </div>
          </div>

          <div *ngSwitchCase="'DOB_CHANGE'">
            <div class="row">
              <label>Date of Birth</label>
              <input type="date" [(ngModel)]="dob" name="dob" required />
            </div>
          </div>

          <div *ngSwitchCase="'ADDRESS_CHANGE'">
            <div class="row">
              <label>Address Line 1</label>
              <input type="text" [(ngModel)]="addrLine1" name="addrLine1" required />
            </div>
            <div class="row">
              <label>Address Line 2 (optional)</label>
              <input type="text" [(ngModel)]="addrLine2" name="addrLine2" />
            </div>
            <div class="row">
              <label>City</label>
              <input type="text" [(ngModel)]="city" name="city" required />
            </div>
            <div class="row">
              <label>State</label>
              <input type="text" [(ngModel)]="state" name="state" required />
            </div>
            <div class="row">
              <label>Postal Code</label>
              <input type="text" [(ngModel)]="postalCode" name="postalCode" required />
            </div>
            <div class="row">
              <label>Country</label>
              <input type="text" [(ngModel)]="country" name="country" required />
            </div>
          </div>

          <div *ngSwitchDefault class="muted">Select a request type to enter details.</div>
        </ng-container>

        <div class="row">
          <label>Documents</label>
          <input type="file" (change)="onFilesSelected($event)" multiple />
          <small class="help-text">Upload mandatory supporting documents (Aadhar/PAN/Address).</small>
        </div>

        <button type="submit" [disabled]="submitting || !type || !userId || selectedFiles.length === 0">Submit Request</button>
      </form>
    </div>

    <!-- My requests -->
    <div class="card">
      <h3>My Requests ({{ myRequests.length }})</h3>
      <button type="button" (click)="loadMine()" [disabled]="loading">Refresh</button>
      <div *ngIf="myRequests.length === 0" class="muted">No requests found.</div>

      <table *ngIf="myRequests.length > 0" class="req-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Status</th>
            <th>Admin Comment</th>
            <th>Submitted</th>
            <th>Reviewed</th>
            <th>Docs</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let r of myRequests">
            <td>{{ r.id | slice:0:8 }}…</td>
            <td>{{ r.type }}</td>
            <td>{{ r.status }}</td>
            <td>{{ r.adminComment || '-' }}</td>
            <td>{{ r.submittedAt || '-' }}</td>
            <td>{{ r.reviewedAt || '-' }}</td>
            <td>
              <ng-container *ngIf="r.documents?.length; else noDocs">
                <span>{{ r.documents?.length }} file(s)</span>
              </ng-container>
              <ng-template #noDocs>-</ng-template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    .card { border:1px solid #e0e0e0; border-radius:8px; padding:1rem; margin-bottom:1rem; background:#fff; }
    .row { display:flex; gap:.75rem; align-items:flex-start; margin-bottom:.75rem; flex-wrap:wrap; }
    .row label { width: 160px; padding-top:.4rem; }
    input, select, textarea { flex: 1 1 380px; padding:.45rem .55rem; border:1px solid #ccc; border-radius:4px; }
    button { padding:.45rem .9rem; border:none; background:#0d6efd; color:#fff; border-radius:4px; cursor:pointer; }
    button[disabled] { opacity:.6; cursor:not-allowed; }
    .loading { color:#555; }
    .error { color:#b00020; margin-bottom:.5rem; }
    .success { color:#1b5e20; margin-bottom:.5rem; }
    .muted { color:#777; }
    table.req-table { width: 100%; border-collapse: collapse; margin-top:.5rem; }
    table.req-table th, table.req-table td { border:1px solid #eee; padding:.5rem; text-align:left; }
  `]
})
export class SelfServiceRequestsComponent implements OnInit {
  userId: string | null = null;
  loading = false;
  submitting = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  types = Object.values(SelfServiceRequestType);
  type: SelfServiceRequestType = SelfServiceRequestType.NAME_CHANGE;
  payloadJson = '';
  selectedFiles: File[] = [];

  // Structured inputs (auto-converted to JSON on submit)
  nameFirst = '';
  nameMiddle = '';
  nameLast = '';
  dob = '';
  addrLine1 = '';
  addrLine2 = '';
  city = '';
  state = '';
  postalCode = '';
  country = '';

  myRequests: SelfServiceRequestDto[] = [];

  constructor(
    private selfService: SelfServiceService,
    private auth: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.userId = this.auth.getIdentityClaims()?.sub || null;

    // Preselect request type from query param (?type=NAME_CHANGE|DOB_CHANGE|ADDRESS_CHANGE)
    const qpType = this.route.snapshot.queryParamMap.get('type');
    if (qpType && this.types.includes(qpType as any)) {
      this.type = qpType as any;
    }

    if (!this.userId) {
      this.errorMessage = 'User not found. Please log in again.';
      return;
    }
    this.loadMine();
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.selectedFiles = Array.from(input.files);
    }
  }

  submit(): void {
    if (!this.userId || !this.type || this.selectedFiles.length === 0) return;
    this.errorMessage = null; this.successMessage = null;
    this.submitting = true;

    // Build JSON payload from structured fields
    this.payloadJson = this.buildPayload();

    this.selfService.submitRequestMultipart({
      userId: this.userId,
      type: this.type,
      payloadJson: (this.payloadJson || '').trim() || null,
      documents: this.selectedFiles
    }).subscribe({
      next: (resp) => {
        this.successMessage = `Request submitted (${resp.type}) and pending review.`;
        this.submitting = false;
        this.payloadJson = '';
        this.selectedFiles = [];
        // Reset structured fields
        this.nameFirst = '';
        this.nameMiddle = '';
        this.nameLast = '';
        this.dob = '';
        this.addrLine1 = '';
        this.addrLine2 = '';
        this.city = '';
        this.state = '';
        this.postalCode = '';
        this.country = '';
        this.loadMine();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to submit request (ensure documents are attached).';
        this.submitting = false;
      }
    });
  }

  loadMine(): void {
    if (!this.userId) return;
    this.loading = true;
    this.errorMessage = null; this.successMessage = null;
    this.selfService.listMyRequests(this.userId).subscribe({
      next: (list) => {
        this.myRequests = list || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load requests.';
        this.loading = false;
      }
    });
  }

  private buildPayload(): string {
    try {
      switch (this.type) {
        case SelfServiceRequestType.NAME_CHANGE:
          return JSON.stringify({
            name: {
              firstName: (this.nameFirst || '').trim(),
              middleName: (this.nameMiddle || '').trim() || undefined,
              lastName: (this.nameLast || '').trim()
            }
          });
        case SelfServiceRequestType.DOB_CHANGE:
          return JSON.stringify({
            dateOfBirth: this.dob || null
          });
        case SelfServiceRequestType.ADDRESS_CHANGE:
          return JSON.stringify({
            address: {
              line1: (this.addrLine1 || '').trim(),
              line2: (this.addrLine2 || '').trim() || undefined,
              city: (this.city || '').trim(),
              state: (this.state || '').trim(),
              postalCode: (this.postalCode || '').trim(),
              country: (this.country || '').trim()
            }
          });
        default:
          return '';
      }
    } catch {
      return '';
    }
  }
}
