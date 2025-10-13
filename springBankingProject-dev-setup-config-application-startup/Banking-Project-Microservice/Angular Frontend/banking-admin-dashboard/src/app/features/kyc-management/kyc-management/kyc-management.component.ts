import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminKycService, KycApplication, KycReviewStatus } from '../kyc-management.service';

@Component({
  selector: 'app-kyc-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './kyc-management.component.html',
  styleUrls: ['./kyc-management.component.css']
})
export class KycManagementComponent implements OnInit {
  applications: KycApplication[] = [];
  loading = true;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  statuses: KycReviewStatus[] = ['SUBMITTED', 'APPROVED', 'REJECTED'];
  selectedStatus: KycReviewStatus = 'SUBMITTED';

  constructor(private kycService: AdminKycService) {}

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    this.kycService.listApplicationsByStatus(this.selectedStatus).subscribe({
      next: (apps) => {
        this.applications = apps;
        this.loading = false;
        if (apps.length === 0) {
          this.successMessage = `No ${this.selectedStatus.toLowerCase()} applications found.`;
        }
      },
      error: (err) => {
        console.error('Failed to load applications', err);
        this.errorMessage = err?.error?.message || 'Failed to load applications.';
        this.loading = false;
      }
    });
  }

  onStatusChange(status: string): void {
    this.selectedStatus = status as KycReviewStatus;
    this.loadApplications();
  }

  approve(app: KycApplication): void {
    if (!confirm(`Approve KYC application ${app.applicationId} for user ${app.userId}?`)) return;
    this.review(app, 'APPROVED');
  }

  reject(app: KycApplication): void {
    if (!confirm(`Reject KYC application ${app.applicationId} for user ${app.userId}?`)) return;
    const reason = prompt('Enter reason for rejection (optional):') || undefined;
    this.review(app, 'REJECTED', reason);
  }

  private review(app: KycApplication, decision: 'APPROVED' | 'REJECTED', adminComment?: string): void {
    this.errorMessage = null;
    this.successMessage = null;

    this.kycService.reviewApplication(app.applicationId, decision, adminComment).subscribe({
      next: (updated) => {
        this.successMessage = `Application ${updated.applicationId} ${decision.toLowerCase()} successfully.`;
        this.loadApplications();
      },
      error: (err) => {
        console.error('Failed to review application', err);
        this.errorMessage = err?.error?.message || 'Failed to perform review action.';
      }
    });
  }

  download(app: KycApplication, relPath: string): void {
    this.kycService.downloadDocument(app.applicationId, relPath).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        const fileName = relPath.split('/').pop() || 'document';
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Failed to download document', err);
        this.errorMessage = 'Failed to download document.';
      }
    });
  }

  view(app: KycApplication, relPath: string): void {
    this.kycService.downloadDocument(app.applicationId, relPath).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const newWin = window.open(url, '_blank');
        if (!newWin) {
          const a = document.createElement('a');
          const fileName = relPath.split('/').pop() || 'document';
          a.href = url;
          a.download = fileName;
          document.body.appendChild(a);
          a.click();
          a.remove();
        }
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
        }, 60000);
      },
      error: (err) => {
        console.error('Failed to view document', err);
        this.errorMessage = 'Failed to open document.';
      }
    });
  }

  maskAadhar(a: string): string {
    if (!a || a.length < 12) return a;
    return a.slice(0, 4) + ' **** ****';
  }

  maskPan(p: string): string {
    if (!p || p.length !== 10) return p;
    return p.slice(0, 5) + '****' + p.slice(9);
  }

  clearMessages(): void {
    this.errorMessage = null;
    this.successMessage = null;
  }

  trackByAppId(_i: number, app: KycApplication): string {
    return app.applicationId;
  }
}
