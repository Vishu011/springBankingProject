// src/app/features/self-service/self-service-dashboard.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-self-service-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
  <div class="container">
    <h1>Self Service</h1>
    <p class="lead">Manage your profile details, nominees, and submit change requests.</p>

    <div class="grid">
      <a routerLink="/self-service/nominees" class="tile">
        <i class="fas fa-users"></i>
        <div>
          <h3>Nominees</h3>
          <p>Add, edit, and delete your nominees.</p>
        </div>
      </a>

      <a routerLink="/self-service/contact" class="tile">
        <i class="fas fa-envelope-open-text"></i>
        <div>
          <h3>Contact Details</h3>
          <p>Update your email or phone using OTP verification.</p>
        </div>
      </a>

      <a routerLink="/self-service/requests" class="tile">
        <i class="fas fa-file-signature"></i>
        <div>
          <h3>Change Requests</h3>
          <p>Request name, DOB, or address change with documents.</p>
        </div>
      </a>
    </div>

    <small class="help-text">
      Note: Some actions require admin approval and mandatory document upload.
    </small>
  </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    .lead { color: #555; margin-bottom: 1rem; }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 1rem;
      margin-top: 1rem;
    }
    .tile {
      display: flex;
      gap: .75rem;
      align-items: center;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 1rem;
      text-decoration: none;
      color: inherit;
      background: #fff;
      transition: box-shadow .2s ease;
    }
    .tile:hover { box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
    .tile i { font-size: 1.5rem; color: #0d6efd; }
    .tile h3 { margin: 0 0 .25rem 0; font-size: 1.05rem; }
    .tile p { margin: 0; color: #666; font-size: .9rem; }
    .help-text { display:block; margin-top: 1rem; color: #777; }
  `]
})
export class SelfServiceDashboardComponent {}
