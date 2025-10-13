// banking-admin-dashboard/src/app/features/self-service-requests/self-service-request-detail.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { SelfServiceRequestsAdminService, AdminSelfServiceRequest } from './self-service-requests.service';

@Component({
  selector: 'app-self-service-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  template: `
  <div class="container">
    <button class="btn btn-link mb-2" (click)="goBack()">← Back to list</button>
    <h2>User Profile Request</h2>

    <div *ngIf="loading" class="muted">Loading...</div>
    <div *ngIf="error" class="error">{{ error }}</div>
    <div *ngIf="success" class="success">{{ success }}</div>

    <ng-container *ngIf="!loading && item">
      <div class="card">
        <div class="card-body">
          <div class="grid">
            <div>
              <div class="label">Request ID</div>
              <div class="value mono">{{ item.requestId }}</div>
            </div>
            <div>
              <div class="label">User ID</div>
              <div class="value mono">{{ item.userId }}</div>
            </div>
            <div>
              <div class="label">Type</div>
              <div class="value">{{ item.type }}</div>
            </div>
            <div>
              <div class="label">Status</div>
              <div class="value"><span class="badge" [ngClass]="statusClass(item.status)">{{ item.status }}</span></div>
            </div>
            <div>
              <div class="label">Submitted At</div>
              <div class="value">{{ item.submittedAt || '-' }}</div>
            </div>
            <div>
              <div class="label">Reviewed At</div>
              <div class="value">{{ item.reviewedAt || '-' }}</div>
            </div>
          </div>
        </div>
      </div>

    <!-- Requested changes preview (Form-like comparison) -->
    <div class="card">
      <div class="card-header"><strong>Requested Changes</strong></div>
      <div class="card-body">
        <ng-container [ngSwitch]="item?.type">
          <!-- Name Change -->
          <div *ngSwitchCase="'NAME_CHANGE'">
            <div class="grid2">
              <div>
                <div class="label">Current First Name</div>
                <div class="value">{{ currentUser?.firstName || '-' }}</div>
              </div>
              <div>
                <div class="label">Requested First Name</div>
                <div class="value highlight">{{ requestedName.firstName || '-' }}</div>
              </div>

              <div>
                <div class="label">Current Middle Name</div>
                <div class="value">{{ currentUser?.middleName || '-' }}</div>
              </div>
              <div>
                <div class="label">Requested Middle Name</div>
                <div class="value highlight">{{ requestedName.middleName || '-' }}</div>
              </div>

              <div>
                <div class="label">Current Last Name</div>
                <div class="value">{{ currentUser?.lastName || '-' }}</div>
              </div>
              <div>
                <div class="label">Requested Last Name</div>
                <div class="value highlight">{{ requestedName.lastName || '-' }}</div>
              </div>
            </div>
          </div>

          <!-- DOB Change -->
          <div *ngSwitchCase="'DOB_CHANGE'">
            <div class="grid2">
              <div>
                <div class="label">Current DOB</div>
                <div class="value">{{ currentUser?.dateOfBirth || '-' }}</div>
              </div>
              <div>
                <div class="label">Requested DOB</div>
                <div class="value highlight">{{ requestedDob || '-' }}</div>
              </div>
            </div>
          </div>

          <!-- Address Change -->
          <div *ngSwitchCase="'ADDRESS_CHANGE'">
            <div class="grid2">
              <div>
                <div class="label">Current Address</div>
                <div class="value">
                  <div>{{ currentAddress?.line1 || '-' }}</div>
                  <div *ngIf="currentAddress?.line2">{{ currentAddress?.line2 }}</div>
                  <div>{{ currentAddress?.city || '-' }}, {{ currentAddress?.state || '-' }}</div>
                  <div>{{ currentAddress?.postalCode || '-' }}, {{ currentAddress?.country || '-' }}</div>
                </div>
              </div>
              <div>
                <div class="label">Requested Address</div>
                <div class="value highlight">
                  <div>{{ requestedAddress?.line1 || '-' }}</div>
                  <div *ngIf="requestedAddress?.line2">{{ requestedAddress?.line2 }}</div>
                  <div>{{ requestedAddress?.city || '-' }}, {{ requestedAddress?.state || '-' }}</div>
                  <div>{{ requestedAddress?.postalCode || '-' }}, {{ requestedAddress?.country || '-' }}</div>
                </div>
              </div>
            </div>
          </div>

          <div *ngSwitchDefault class="muted">Unsupported request type.</div>
        </ng-container>
      </div>
    </div>

      <!-- Documents -->
      <div class="card">
        <div class="card-header"><strong>Documents</strong></div>
        <div class="card-body">
          <div *ngIf="docList.length === 0" class="muted">No documents found.</div>
          <ul *ngIf="docList.length > 0">
            <li *ngFor="let d of docList" class="doc-row">
              <span>{{ displayDocName(d) }}</span>
              <button class="btn btn-sm btn-outline-primary ms-2" title="View" (click)="view(d)" [disabled]="downloading">
                👁️ View
              </button>
            </li>
          </ul>
        </div>
      </div>

      <!-- Approve / Reject -->
      <div class="card" *ngIf="item.status === 'SUBMITTED'">
        <div class="card-header"><strong>Review</strong></div>
        <div class="card-body">
          <div class="row mb-2">
            <label>Admin Comment (required for rejection)</label>
            <textarea [(ngModel)]="adminComment" rows="3" placeholder="Provide reason for decision"></textarea>
          </div>
          <div class="row actions">
            <button class="btn btn-success" (click)="approve()" [disabled]="acting">Approve</button>
            <button class="btn btn-danger" (click)="reject()" [disabled]="acting || !adminComment?.trim()">Reject</button>
          </div>
        </div>
      </div>
    </ng-container>
  </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    .muted { color: #777; }
    .error { color: #b00020; margin-bottom:.5rem; }
    .success { color: #1b5e20; margin-bottom:.5rem; }
    .card { border:1px solid #e0e0e0; border-radius:8px; margin-bottom:1rem; background:#fff; }
    .card-header { padding:.6rem .9rem; background:#f7f7f7; border-bottom:1px solid #eee; }
    .card-body { padding:.9rem; }
    .grid { display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap:.75rem; }
    .label { font-size:.85rem; color:#666; }
    .value { font-weight:600; }
    .highlight { background: #fff7e6; padding: .2rem .4rem; border-radius: 4px; display: inline-block; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
    .badge { padding:.2rem .5rem; border-radius: 4px; }
    .badge.SUBMITTED { background:#fff3cd; color:#664d03; border:1px solid #ffe69c; }
    .badge.APPROVED { background:#d1e7dd; color:#0f5132; border:1px solid #badbcc; }
    .badge.REJECTED { background:#f8d7da; color:#842029; border:1px solid #f5c2c7; }
    .json-view { background:#0f172a; color:#e2e8f0; padding:.75rem; border-radius:6px; overflow:auto; }
    .row label { display:block; margin-bottom:.35rem; }
    textarea { width:100%; padding:.45rem .55rem; border:1px solid #ccc; border-radius:4px; }
    .actions { display:flex; gap:.75rem; }
    .btn { padding:.45rem .9rem; border:none; border-radius:4px; cursor:pointer; }
    .btn-success { background:#198754; color:#fff; }
    .btn-danger { background:#dc3545; color:#fff; }
    .btn-outline-primary { background:#fff; border:1px solid #0d6efd; color:#0d6efd; }
    .btn.btn-link { text-decoration:none; border:none; background:none; color:#0d6efd; padding:0; }
    .ms-2 { margin-left:.5rem; }
    .doc-row { display: flex; align-items: center; gap: .5rem; }
  `]
})
export class SelfServiceRequestDetailComponent implements OnInit {
  requestId: string | null = null;
  item: AdminSelfServiceRequest | null = null;
  prettyPayload = '';
  payloadObj: any = null;

  // Current user profile (for comparison)
  currentUser: { userId: string; firstName?: string; middleName?: string; lastName?: string; email?: string; dateOfBirth?: string; address?: any } | null = null;

  // Requested values parsed from payload
  requestedName = { firstName: '', middleName: '', lastName: '' };
  requestedDob: string | null = null;
  requestedAddress: { line1?: string; line2?: string; city?: string; state?: string; postalCode?: string; country?: string } | null = null;
  currentAddress: { line1?: string; line2?: string; city?: string; state?: string; postalCode?: string; country?: string } | null = null;

  docList: Array<string | { fileName?: string; relativePath?: string; size?: number }> = [];
  adminComment = '';
  loading = false;
  error: string | null = null;
  success: string | null = null;
  acting = false;
  downloading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: SelfServiceRequestsAdminService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.requestId = this.route.snapshot.paramMap.get('id');
    if (!this.requestId) {
      this.error = 'Invalid request id.';
      return;
    }
    this.load();
  }

  load(): void {
    if (!this.requestId) return;
    this.loading = true; this.error = null; this.success = null;
    this.api.getOne(this.requestId).subscribe({
      next: (r) => {
        this.item = r;
        // Parse and store payload
        this.prettyPayload = this.formatPayload(r?.payloadJson);
        this.payloadObj = this.parsePayload(r?.payloadJson);

        // Derive requested fields depending on type
        this.extractRequestedFields();

        // Fetch current user for comparison
        if (r?.userId) {
          this.fetchCurrentUser(r.userId);
        }

        // Documents
        this.docList = this.normalizeDocs(r?.documents);
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load request.';
        this.loading = false;
      }
    });
  }

  formatPayload(payloadJson?: string | null): string {
    if (!payloadJson) return '';
    try {
      const obj = JSON.parse(payloadJson);
      return JSON.stringify(obj, null, 2);
    } catch {
      return payloadJson;
    }
  }

  parsePayload(payloadJson?: string | null): any {
    if (!payloadJson) return null;
    try {
      return JSON.parse(payloadJson);
    } catch {
      return null;
    }
  }

  extractRequestedFields(): void {
    if (!this.item) return;
    const type = this.item.type;
    const p = this.payloadObj || {};
    if (type === 'NAME_CHANGE') {
      // Support both nested { name: {..} } or flat { firstName, ... }
      const name = p.name || p;
      this.requestedName = {
        firstName: (name.firstName || '').toString(),
        middleName: (name.middleName || '').toString(),
        lastName: (name.lastName || '').toString()
      };
    } else if (type === 'DOB_CHANGE') {
      this.requestedDob = (p.dateOfBirth || p.dob || '').toString();
    } else if (type === 'ADDRESS_CHANGE') {
      this.requestedAddress = p.address || p;
    }
  }

  fetchCurrentUser(userId: string): void {
    const url = `${environment.apiUrl}/auth/user/${encodeURIComponent(userId)}`;
    this.http.get<any>(url).subscribe({
      next: (u) => {
        // Normalize keys used in UI
        this.currentUser = {
          userId: u?.userId || userId,
          firstName: u?.firstName || u?.firstname || '',
          middleName: u?.middleName || u?.middlename || '',
          lastName: u?.lastName || u?.lastname || '',
          email: u?.email || '',
          dateOfBirth: u?.dateOfBirth || u?.dob || ''
        };
        // Address normalization
        const addr = u?.address || {
          line1: u?.addressLine1,
          line2: u?.addressLine2,
          city: u?.city,
          state: u?.state,
          postalCode: u?.postalCode || u?.pincode,
          country: u?.country
        };
        this.currentAddress = addr;
      },
      error: () => {
        // Keep UI functional even if profile fails to load
        this.currentUser = { userId };
      }
    });
  }

  normalizeDocs(docs: any): Array<string | { fileName?: string; relativePath?: string; size?: number }> {
    if (!docs) return [];
    // Backend could return either string[] (relative paths) or array of objects
    if (Array.isArray(docs)) return docs;
    return [];
  }

  displayDocName(d: any): string {
    if (!d) return '-';
    if (typeof d === 'string') {
      const parts = d.split('/'); return parts[parts.length - 1];
    }
    return d.fileName || d.relativePath || '-';
  }

  view(d: any): void {
    if (!this.requestId) return;
    const relativePath = typeof d === 'string' ? d : (d.relativePath || '');
    if (!relativePath) return;
    this.downloading = true;
    this.api.downloadDocument(this.requestId, relativePath).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        // revoke after a short delay to keep tab content
        setTimeout(() => window.URL.revokeObjectURL(url), 10000);
        this.downloading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to open document.';
        this.downloading = false;
      }
    });
  }

  approve(): void {
    if (!this.requestId) return;
    this.acting = true; this.error = null; this.success = null;
    this.api.approve(this.requestId, { adminComment: this.adminComment || null, reviewerId: null }).subscribe({
      next: (res) => {
        this.success = 'Request approved and profile updated.';
        this.item = res;
        this.acting = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to approve request.';
        this.acting = false;
      }
    });
  }

  reject(): void {
    if (!this.requestId) return;
    if (!this.adminComment || !this.adminComment.trim()) {
      this.error = 'Admin comment is required to reject a request.';
      return;
    }
    this.acting = true; this.error = null; this.success = null;
    this.api.reject(this.requestId, { adminComment: this.adminComment.trim(), reviewerId: null }).subscribe({
      next: (res) => {
        this.success = 'Request rejected and user notified.';
        this.item = res;
        this.acting = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reject request.';
        this.acting = false;
      }
    });
  }

  statusClass(s: string | undefined): string {
    return s || 'UNKNOWN';
  }

  goBack(): void {
    this.router.navigateByUrl('/self-service-requests');
  }
}
