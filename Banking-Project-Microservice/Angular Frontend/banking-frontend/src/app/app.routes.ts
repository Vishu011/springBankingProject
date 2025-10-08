// src/app/app.routes.ts (Updated with Layout)

import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { BankingFeaturesComponent } from './features/banking-features/banking-features.component';
import { AccountsListComponent } from './features/accounts/accounts-list/accounts-list.component';
import { TransactionHistoryComponent } from './features/transactions/transaction-history/transaction-history.component';
import { DepositComponent } from './features/transactions/deposit/deposit.component';
import { WithdrawComponent } from './features/transactions/withdraw/withdraw.component';
import { TransferComponent } from './features/transactions/transfer/transfer.component';
import { LoanApplicationComponent } from './features/loans/loan-application/loan-application.component';
import { LoanDetailsComponent } from './features/loans/loan-details/loan-details.component';
import { CardIssuanceComponent } from './features/cards/card-issuance/card-issuance.component';
import { CardManagementComponent } from './features/cards/card-management/card-management.component';

import { authGuard } from './core/guards/auth.guard';
import { AuthenticatedLayoutComponent } from './features/layout/authenticated-layout/authenticated-layout.component';
import { SidebarComponent } from './features/layout/sidebar/sidebar.component';
import { NotificationListComponent } from './features/notifications/notification-list/notification-list.component';
import { KycFormComponent } from './features/kyc/kyc-form/kyc-form.component';
import { kycVerifiedGuard } from './core/guards/kyc.guard';

export const routes: Routes = [
    // Public routes (no layout)
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent },

    // Dashboard route (authenticated but without layout)
    {
        path: 'dashboard',
        component: DashboardComponent,
        canActivate: [authGuard]
    },

    // Authenticated routes (with layout)
    {
        path: '',
        component: AuthenticatedLayoutComponent,
        canActivate: [authGuard],
        children: [
            { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
            { path: 'banking-features', component: BankingFeaturesComponent, canActivate: [kycVerifiedGuard] },
            { path: 'accounts', component: AccountsListComponent, canActivate: [kycVerifiedGuard] },
            { path: 'transactions/history', component: TransactionHistoryComponent, canActivate: [kycVerifiedGuard] },
            { path: 'transactions/deposit', component: DepositComponent, canActivate: [kycVerifiedGuard] },
            { path: 'banking/notifications', component: NotificationListComponent, canActivate: [kycVerifiedGuard] }, // New route for notifications
            { path: 'transactions/withdraw', component: WithdrawComponent, canActivate: [kycVerifiedGuard] },
            { path: 'transactions/transfer', component: TransferComponent, canActivate: [kycVerifiedGuard] },
            { path: 'loans', redirectTo: '/loans/history', pathMatch: 'full' },
            { path: 'loans/history', component: LoanDetailsComponent, canActivate: [kycVerifiedGuard] },
            { path: 'loans/apply', component: LoanApplicationComponent, canActivate: [kycVerifiedGuard] },
            { path: 'cards', redirectTo: '/cards/manage', pathMatch: 'full' },
            { path: 'cards/manage', component: CardManagementComponent, canActivate: [kycVerifiedGuard] },
            { path: 'cards/issue', component: CardIssuanceComponent, canActivate: [kycVerifiedGuard] },
            { path: 'kyc', component: KycFormComponent },

        ]
    },

    // Wildcard route
    { path: '**', redirectTo: '/dashboard' },
];
