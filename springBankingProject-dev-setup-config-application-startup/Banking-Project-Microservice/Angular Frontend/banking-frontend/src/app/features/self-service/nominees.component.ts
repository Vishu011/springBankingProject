// src/app/features/self-service/nominees.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SelfServiceService } from './self-service.service';
import { AuthService } from '../../core/services/auth.service';
import {
  NomineeResponse,
  NomineeCreateRequest,
  NomineeUpdateRequest,
  Gender,
  NomineeRelationship
} from '../../shared/models/self-service.model';

@Component({
  selector: 'app-self-service-nominees',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="container">
    <h2>Nominees</h2>
    <div *ngIf="loading" class="loading">Loading...</div>
    <div *ngIf="errorMessage" class="error">{{ errorMessage }}</div>
    <div *ngIf="successMessage" class="success">{{ successMessage }}</div>

    <!-- Add nominee form -->
    <div class="card">
      <h3>Add Nominee</h3>
      <form (ngSubmit)="createNominee()" #createForm="ngForm">
        <div class="row">
          <label>Name</label>
          <input type="text" [(ngModel)]="createModel.name" name="name" required />
        </div>
        <div class="row">
          <label>Age</label>
          <input type="number" [(ngModel)]="createModel.age" name="age" min="0" required />
        </div>
        <div class="row">
          <label>Gender</label>
          <select [(ngModel)]="createModel.gender" name="gender" required>
            <option *ngFor="let g of genders" [value]="g">{{ g }}</option>
          </select>
        </div>
        <div class="row">
          <label>Relationship</label>
          <select [(ngModel)]="createModel.relationship" name="relationship" required>
            <option *ngFor="let r of relationships" [value]="r">{{ r }}</option>
          </select>
        </div>
        <div class="row">
          <label>Share (%) - optional</label>
          <input type="number" [(ngModel)]="createModel.percentageShare" name="percentageShare" min="0" max="100" />
        </div>
        <button type="submit" [disabled]="!createForm.valid || !userId">Add Nominee</button>
      </form>
    </div>

    <!-- Existing nominees list -->
    <div class="card" *ngIf="!loading">
      <h3>My Nominees ({{ nominees.length }})</h3>
      <div *ngIf="nominees.length === 0" class="muted">No nominees yet.</div>
      <table *ngIf="nominees.length > 0" class="nominee-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Age</th>
            <th>Gender</th>
            <th>Relationship</th>
            <th>Share (%)</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let n of nominees">
            <td>
              <span *ngIf="!isEditing(n.id)">{{ n.name }}</span>
              <input *ngIf="isEditing(n.id)" type="text" [(ngModel)]="editModel.name" />
            </td>
            <td>
              <span *ngIf="!isEditing(n.id)">{{ n.age }}</span>
              <input *ngIf="isEditing(n.id)" type="number" [(ngModel)]="editModel.age" min="0" />
            </td>
            <td>
              <span *ngIf="!isEditing(n.id)">{{ n.gender }}</span>
              <select *ngIf="isEditing(n.id)" [(ngModel)]="editModel.gender">
                <option *ngFor="let g of genders" [value]="g">{{ g }}</option>
              </select>
            </td>
            <td>
              <span *ngIf="!isEditing(n.id)">{{ n.relationship }}</span>
              <select *ngIf="isEditing(n.id)" [(ngModel)]="editModel.relationship">
                <option *ngFor="let r of relationships" [value]="r">{{ r }}</option>
              </select>
            </td>
            <td>
              <span *ngIf="!isEditing(n.id)">{{ n.percentageShare ?? '-' }}</span>
              <input *ngIf="isEditing(n.id)" type="number" [(ngModel)]="editModel.percentageShare" min="0" max="100" />
            </td>
            <td class="actions">
              <button *ngIf="!isEditing(n.id)" (click)="beginEdit(n)">Edit</button>
              <button *ngIf="isEditing(n.id)" (click)="saveEdit(n)">Save</button>
              <button *ngIf="isEditing(n.id)" (click)="cancelEdit()">Cancel</button>
              <button (click)="deleteNominee(n)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    .row { display:flex; gap:.75rem; align-items:center; margin-bottom:.5rem; }
    .row label { width: 150px; }
    .card { border: 1px solid #e0e0e0; border-radius: 8px; padding: 1rem; margin-bottom: 1rem; background: #fff; }
    .loading { color: #555; }
    .error { color: #b00020; margin-bottom: .5rem; }
    .success { color: #1b5e20; margin-bottom: .5rem; }
    .muted { color: #777; }
    table.nominee-table { width: 100%; border-collapse: collapse; }
    table.nominee-table th, table.nominee-table td { border: 1px solid #eee; padding: .5rem; text-align: left; }
    .actions button { margin-right: .5rem; }
  `]
})
export class SelfServiceNomineesComponent implements OnInit {
  userId: string | null = null;
  nominees: NomineeResponse[] = [];
  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  genders = Object.values(Gender);
  relationships = Object.values(NomineeRelationship);

  createModel: NomineeCreateRequest = {
    userId: '',
    name: '',
    age: 0,
    gender: Gender.MALE,
    relationship: NomineeRelationship.OTHER,
    percentageShare: null
  };

  editingId: string | null = null;
  editModel: NomineeUpdateRequest = {
    name: '',
    age: 0,
    gender: Gender.MALE,
    relationship: NomineeRelationship.OTHER,
    percentageShare: null
  };

  constructor(
    private selfService: SelfServiceService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.userId = this.auth.getIdentityClaims()?.sub || null;
    if (!this.userId) {
      this.errorMessage = 'User not found. Please log in again.';
      return;
    }
    this.createModel.userId = this.userId;
    this.loadNominees();
  }

  loadNominees(): void {
    if (!this.userId) return;
    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.selfService.listNominees(this.userId).subscribe({
      next: (list) => {
        this.nominees = list || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load nominees.';
        this.loading = false;
      }
    });
  }

  createNominee(): void {
    if (!this.userId) return;
    this.errorMessage = null;
    this.successMessage = null;
    this.selfService.createNominee(this.createModel).subscribe({
      next: (n) => {
        this.successMessage = 'Nominee added successfully.';
        this.createModel = {
          userId: this.userId!,
          name: '',
          age: 0,
          gender: Gender.MALE,
          relationship: NomineeRelationship.OTHER,
          percentageShare: null
        };
        this.loadNominees();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to add nominee.';
      }
    });
  }

  beginEdit(n: NomineeResponse): void {
    this.editingId = n.id;
    this.editModel = {
      name: n.name,
      age: n.age,
      gender: n.gender,
      relationship: n.relationship,
      percentageShare: n.percentageShare ?? null
    };
  }

  isEditing(id: string): boolean {
    return this.editingId === id;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(n: NomineeResponse): void {
    if (!this.userId || !this.editingId) return;
    this.errorMessage = null;
    this.successMessage = null;
    this.selfService.updateNominee(this.editingId, this.editModel).subscribe({
      next: () => {
        this.successMessage = 'Nominee updated successfully.';
        this.editingId = null;
        this.loadNominees();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to update nominee.';
      }
    });
  }

  deleteNominee(n: NomineeResponse): void {
    if (!this.userId) return;
    if (!confirm(`Delete nominee ${n.name}?`)) return;
    this.errorMessage = null;
    this.successMessage = null;
    this.selfService.deleteNominee(n.id, this.userId).subscribe({
      next: () => {
        this.successMessage = 'Nominee deleted.';
        this.loadNominees();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to delete nominee.';
      }
    });
  }
}
