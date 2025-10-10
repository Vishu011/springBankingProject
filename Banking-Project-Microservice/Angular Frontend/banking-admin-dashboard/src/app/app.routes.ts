// banking-admin-dashboard/src/app/app.routes.ts

import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { AdminDashboardComponent } from './features/admin-dashboard/admin-dashboard.component';
import { UnauthorizedComponent } from './features/auth/unauthorized/unauthorized.component';

import { authGuard } from './core/guards/auth.guard';
import { LoanManagementComponent } from './features/loan-management/loan-management/loan-management.component';
import { AccountManagementComponent } from './features/account-management/account-management/account-management.component';
import { UserManagementComponent } from './features/user-management/user-management/user-management.component';
import { KycManagementComponent } from './features/kyc-management/kyc-management/kyc-management.component';

export const routes: Routes = [
    { path: '', redirectTo: '/admin-dashboard', pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    { path: 'unauthorized', component: UnauthorizedComponent },
    { path: 'admin-dashboard', component: AdminDashboardComponent, canActivate: [authGuard] },
    { path: 'loan-management', component: LoanManagementComponent, canActivate: [authGuard] },
    { path: 'account-management', component: AccountManagementComponent, canActivate: [authGuard] },
    { path: 'user-management', component: UserManagementComponent, canActivate: [authGuard] }, // New route for user management
    { path: 'kyc-management', component: KycManagementComponent, canActivate: [authGuard] },
    { path: 'salary-applications', canActivate: [authGuard], loadComponent: () => import('./features/salary-applications/salary-applications-list/salary-applications-list.component').then(m => m.SalaryApplicationsListComponent) },
    { path: 'salary-applications/:id', canActivate: [authGuard], loadComponent: () => import('./features/salary-applications/salary-application-detail/salary-application-detail.component').then(m => m.SalaryApplicationDetailComponent) },
    { path: '**', redirectTo: '/admin-dashboard' },
];
