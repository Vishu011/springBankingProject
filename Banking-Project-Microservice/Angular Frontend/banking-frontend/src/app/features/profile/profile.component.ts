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
  <div class="profile-container">
  <h2>My Profile</h2>

  <!-- Messages -->
  <div *ngIf="loading" class="muted">Loading...</div>
  <div *ngIf="error" class="alert error">{{ error }}</div>
  <div *ngIf="success" class="alert success">{{ success }}</div>

  <!-- Dashboard Grid -->
  <div class="dashboard-grid" *ngIf="!loading && profile">
    
    <!-- Profile Photo Card -->
    <div class="card photo-card">
      <div class="photo-box">
        <img [src]="photoSrc" alt="Profile Photo" (error)="onImgError($event)">
      </div>
      <input type="file" (change)="onPhotoSelected($event)">
      <button class="btn upload-btn" [disabled]="uploading || !selectedPhoto" (click)="uploadPhoto()">
        {{ uploading ? 'Uploading...' : 'Upload Photo' }}
      </button>
    </div>

    <!-- Basic Info Card -->
    <div class="card info-card">
      <div class="card-header">
        <span>Basic Information</span>
        <span *ngIf="profile.kycStatus" class="badge" [ngClass]="(profile.kycStatus?.toLowerCase() || '')">{{ profile.kycStatus }}</span>
      </div>
      <div class="card-body">
        <div class="row"><span class="label">Full Name</span><span class="value">{{ fullName() }}</span><button class="icon-btn" (click)="editName()">✏️</button></div>
        <div class="row"><span class="label">DOB</span><span class="value">{{ profile.dateOfBirth || '-' }}</span><button class="icon-btn" (click)="editDob()">✏️</button></div>
        <div class="row"><span class="label">Email</span><span class="value">{{ profile.email || '-' }}</span><button class="icon-btn" (click)="editEmail()">✏️</button></div>
        <div class="row"><span class="label">Phone</span><span class="value">{{ profile.phoneNumber || '-' }}</span><button class="icon-btn" (click)="editPhone()">✏️</button></div>
        <div class="row"><span class="label">Address</span><span class="value">{{ profile.address || '-' }}</span><button class="icon-btn" (click)="editAddress()">✏️</button></div>
        <div class="row"><span class="label">Aadhaar</span><span class="value">{{ profile.aadharNumber || '-' }}</span></div>
        <div class="row"><span class="label">PAN</span><span class="value">{{ profile.panNumber || '-' }}</span></div>
      </div>
    </div>

    <!-- Nominees Card -->
    <div class="card nominees-card">
      <div class="card-header">
        <span>Nominees ({{ nominees.length }})</span>
        <button class="btn btn-sm" (click)="goNominees()">Manage</button>
      </div>
      <div class="card-body">
        <div *ngIf="nominees.length === 0" class="muted">No nominees added.</div>
        <table *ngIf="nominees.length > 0" class="table">
          <thead>
            <tr><th>Name</th><th>Age</th><th>Gender</th><th>Relationship</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let n of nominees">
              <td>{{ n.name }}</td><td>{{ n.age }}</td><td>{{ n.gender }}</td><td>{{ n.relationship }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

  </div>
</div>

  `,
  styles: [`
    .container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
  font-family: 'Inter', Arial, sans-serif;
  color: #333;
}

h2 {
  text-align: center;
  color: #A50034;
  font-size: 2rem;
  margin-bottom: 2rem;
  font-weight: 700;
}

/* Messages */
.muted {
  color: #6c757d;
  text-align: center;
  margin-bottom: 1rem;
}

.error {
  color: #b00020;
  background: #fdecea;
  padding: 0.8rem 1rem;
  border-radius: 8px;
  text-align: center;
  margin-bottom: 1rem;
  font-weight: 500;
}

.success {
  color: #155724;
  background: #d4edda;
  padding: 0.8rem 1rem;
  border-radius: 8px;
  text-align: center;
  margin-bottom: 1rem;
  font-weight: 500;
}

/* Grid layout */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

/* Cards */
.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.05);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(0,0,0,0.08);
}

.card-header {
  padding: 0.75rem 1rem;
  background: #f7f7f7;
  border-bottom: 1px solid #eee;
  font-weight: 600;
  font-size: 1rem;
}

.card-body {
  padding: 1rem;
}

/* Rows inside cards */
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.8rem;
}

.label {
  color: #555;
  font-weight: 500;
  width: 140px;
}

.value {
  flex: 1;
  font-weight: 600;
  word-break: break-word;
}

.icon-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 1.1rem;
  margin-left: 0.5rem;
  transition: transform 0.2s ease;
}

.icon-btn:hover {
  transform: scale(1.1);
}

/* Buttons */
.btn {
  padding: 0.35rem 0.7rem;
  border: none;
  border-radius: 6px;
  background: #0d6efd;
  color: #fff;
  cursor: pointer;
  transition: background 0.3s ease;
}

.btn:hover {
  background: #0b5ed7;
}

.btn.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.85rem;
}

/* Manage nominees button */
.ms-2 {
  margin-left: 0.5rem;
}

/* Tables */
.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
}

.table th, .table td {
  border: 1px solid #eee;
  padding: 0.5rem;
  text-align: left;
}

.table th {
  background: #f2f2f2;
  font-weight: 600;
}

/* Profile photo card */
.photo-card .photo-box {
  width: 160px;
  height: 160px;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
  background: #fafafa;
  border: 1px solid #ddd;
}

.photo-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 12px;
}

/* File input row */
.photo-card .row input[type="file"] {
  flex: 1;
}

.photo-card .row button {
  margin-left: 0.5rem;
  background: #28a745;
}

.photo-card .row button:hover {
  background: #218838;
}

/* Responsive tweaks */
@media (max-width: 480px) {
  .label { width: 100px; }
  .row { flex-direction: column; align-items: flex-start; gap: 0.3rem; }
}

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
