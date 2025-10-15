import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { AdminDashboardComponent } from "./features/admin-dashboard/admin-dashboard.component";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AdminDashboardComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'banking-admin-dashboard';
  showShell = true;

  constructor(private authService: AuthService, private router: Router) {
    this.authService.init();
    this.updateShell(this.router.url);
    this.router.events.subscribe((evt) => {
      if (evt instanceof NavigationEnd) {
        this.updateShell(evt.urlAfterRedirects || evt.url);
      }
    });
  }

  private updateShell(url: string): void {
    try {
      const path = (url || '').split('?')[0].split('#')[0];
      this.showShell = !(path === '/login' || path === '/unauthorized');
    } catch {
      this.showShell = true;
    }
  }
}
