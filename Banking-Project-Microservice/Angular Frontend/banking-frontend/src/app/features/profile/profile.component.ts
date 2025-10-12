// src/app/features/profile/profile.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

type UserResponse = {
  userId: string;
  username: string;
  email: string;
  role: string;
  createdAt: string;
  firstName?: string;
  middleName?: string;
  lastName?: string;
  dateOfBirth?: string;
  address?: string;
  phoneNumber?: string;
  kycStatus?: string;
  aadharNumber?: string;
  panNumber?: string;
};

type Nominee = {
  nomineeId: string;
  userId: string;
  name: string;
  age: number;
  gender: string;
  relationship: string;
};

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  template: `
  <div class="container">
    <h2>My Profile</h2>
    <div *ngIf="loading" class="muted">Loading...</div>
    <div *ngIf="error" class="error">{{ error }}</div>
    <div *ngIf="success" class="success">{{ success }}</div>

    <div class="grid" *ngIf="!loading && profile">
      <!-- Photo -->
      <div class="card photo-card">
        <div class="card-header"><strong>Profile Photo</strong></div>
        <div class="card-body">
          <div class="photo-box">
            <img [src]="photoSrc" alt="Profile photo" (error)="onImgError($event)" />
          </div>
          <div class="row">
            <input type="file" (change)="onPhotoSelected($event)" />
            <button class="btn btn-sm" (click)="uploadPhoto()" [disabled]="uploading || !selectedPhoto">Upload</button>
          </div>
          <div *ngIf="uploading" class="muted">Uploading...</div>
        </div>
      </div>

      <!-- Basic Info -->
      <div class="card">
        <div class="card-header"><strong>Basic Information</strong></div>
        <div class="card-body">
          <div class="row">
            <div class="label">Name</div>
            <div class="value">{{ fullName() }}</div>
            <button class="icon-btn" title="Edit name" (click)="editName()">✏️</button>
          </div>
          <div class="row">
            <div class="label">Date of Birth</div>
            <div class="value">{{ profile?.dateOfBirth || '-' }}</div>
            <button class="icon-btn" title="Edit DOB" (click)="editDob()">✏️</button>
          </div>
          <div class="row">
            <div class="label">Email</div>
            <div class="value">{{ profile?.email || '-' }}</div>
            <button class="icon-btn" title="Change Email (OTP)" (click)="editEmail()">✏️</button>
          </div>
          <div class="row">
            <div class="label">Phone</div>
            <div class="value">{{ profile?.phoneNumber || '-' }}</div>
            <button class="icon-btn" title="Change Phone (OTP to email)" (click)="editPhone()">✏️</button>
          </div>
          <div class="row">
            <div class="label">Address</div>
            <div class="value">{{ profile?.address || '-' }}</div>
            <button class="icon-btn" title="Edit address" (click)="editAddress()">✏️</button>
          </div>
          <div class="row">
            <div class="label">Aadhaar</div>
            <div class="value">{{ profile?.aadharNumber || '-' }}</div>
          </div>
          <div class="row">
            <div class="label">PAN</div>
            <div class="value">{{ profile?.panNumber || '-' }}</div>
          </div>
        </div>
      </div>

      <!-- Nominees -->
      <div class="card">
        <div class="card-header">
          <strong>Nominees ({{ nominees.length }})</strong>
          <button class="btn btn-sm ms-2" (click)="goNominees()">Manage</button>
        </div>
        <div class="card-body">
          <div *ngIf="nominees.length === 0" class="muted">No nominees added.</div>
          <table *ngIf="nominees.length > 0" class="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Age</th>
                <th>Gender</th>
                <th>Relationship</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let n of nominees">
                <td>{{ n.name }}</td>
                <td>{{ n.age }}</td>
                <td>{{ n.gender }}</td>
                <td>{{ n.relationship }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    .muted { color: #777; }
    .error { color: #b00020; margin-bottom:.5rem; }
    .success { color: #1b5e20; margin-bottom:.5rem; }
    .grid { display:grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap:1rem; }
    .card { border:1px solid #e0e0e0; border-radius:8px; background:#fff; }
    .card-header { padding:.6rem .9rem; background:#f7f7f7; border-bottom:1px solid #eee; display:flex; align-items:center; }
    .card-body { padding:.9rem; }
    .row { display:flex; align-items:center; gap:.5rem; margin-bottom:.6rem; }
    .label { width: 140px; color:#666; }
    .value { flex: 1; font-weight:600; }
    .icon-btn { border:none; background:transparent; cursor:pointer; font-size: 1rem; }
    .btn { padding:.35rem .7rem; border:none; border-radius:4px; background:#0d6efd; color:#fff; cursor:pointer; }
    .btn.btn-sm { padding:.25rem .5rem; font-size:.85rem; }
    .ms-2 { margin-left:.5rem; }
    .table { width:100%; border-collapse: collapse; }
    .table th, .table td { border:1px solid #eee; padding:.4rem; text-align:left; }
    .photo-card .photo-box { width: 160px; height: 160px; border:1px solid #ddd; border-radius:8px; overflow:hidden; display:flex; align-items:center; justify-content:center; margin-bottom:.5rem; background:#fafafa; }
    .photo-card img { max-width:100%; max-height:100%; display:block; }
  `]
})
export class ProfileComponent implements OnInit {
  profile: UserResponse | null = null;
  nominees: Nominee[] = [];
  loading = false;
  error: string | null = null;
  success: string | null = null;
  selectedPhoto: File | null = null;
  uploading = false;
  photoSrc: string = '';
  private photoObjectUrl: string | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true; this.error = null; this.success = null;
    // Load profile of authenticated user
    this.http.get<UserResponse>(`${environment.apiUrl}/auth/profile`).subscribe({
      next: (u) => {
        this.profile = u;
        this.loading = false;
        this.loadNominees();
        this.loadPhoto();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load profile.';
        this.loading = false;
      }
    });
  }

  loadNominees(): void {
    if (!this.profile?.userId) { this.nominees = []; return; }
    this.http.get<Nominee[]>(`${environment.apiUrl}/self-service/nominees`, { params: { userId: this.profile.userId } }).subscribe({
      next: (list) => this.nominees = list || [],
      error: () => this.nominees = []
    });
  }

  fullName(): string {
    if (!this.profile) return '-';
    const parts = [this.profile.firstName, this.profile.middleName, this.profile.lastName].filter(Boolean);
    return parts.length ? parts.join(' ') : '-';
    }

  photoUrl(): string {
    return this.profile?.userId ? `${environment.apiUrl}/auth/user/${this.profile.userId}/photo` : '';
  }

  loadPhoto(): void {
    const id = this.profile?.userId;
    if (!id) {
      this.photoSrc = this.placeholder();
      return;
    }
    this.http.get(`${environment.apiUrl}/auth/user/${id}/photo`, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        if (this.photoObjectUrl) {
          URL.revokeObjectURL(this.photoObjectUrl);
        }
        this.photoObjectUrl = URL.createObjectURL(blob);
        this.photoSrc = this.photoObjectUrl;
      },
      error: () => {
        this.photoSrc = this.placeholder();
      }
    });
  }

  private placeholder(): string {
    return 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160"><rect width="100%" height="100%" fill="%23f0f0f0"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999" font-size="14">No Photo</text></svg>';
  }

  onImgError(ev: Event): void {
    this.photoSrc = this.placeholder();
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedPhoto = input.files[0];
    }
  }

  uploadPhoto(): void {
    if (!this.profile?.userId || !this.selectedPhoto) return;
    const form = new FormData();
    form.append('file', this.selectedPhoto);
    this.uploading = true; this.error = null; this.success = null;
    this.http.post<void>(`${environment.apiUrl}/auth/user/${this.profile.userId}/photo`, form).subscribe({
      next: () => {
        this.success = 'Photo uploaded.';
        this.uploading = false;
        this.loadPhoto();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Upload failed.';
        this.uploading = false;
      }
    });
  }

  // Pencil actions
  editName(): void { this.goSelfServiceType('NAME_CHANGE'); }
  editDob(): void { this.goSelfServiceType('DOB_CHANGE'); }
  editAddress(): void { this.goSelfServiceType('ADDRESS_CHANGE'); }
  editEmail(): void { this.router.navigateByUrl('/self-service/contact?focus=email'); }
  editPhone(): void { this.router.navigateByUrl('/self-service/contact?focus=phone'); }
  goNominees(): void { this.router.navigateByUrl('/self-service/nominees'); }

  private goSelfServiceType(type: 'NAME_CHANGE' | 'DOB_CHANGE' | 'ADDRESS_CHANGE'): void {
    this.router.navigateByUrl(`/self-service/requests?type=${encodeURIComponent(type)}`);
  }
}
