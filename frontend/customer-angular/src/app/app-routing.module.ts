import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AccountsComponent } from "./pages/accounts/accounts.component";
import { PaymentsComponent } from "./pages/payments/payments.component";
import { BeneficiariesComponent } from "./pages/beneficiaries/beneficiaries.component";
import { LoansComponent } from "./pages/loans/loans.component";
import { CardsComponent } from "./pages/cards/cards.component";

const routes: Routes = [
  { path: "", redirectTo: "accounts", pathMatch: "full" },
  { path: "accounts", component: AccountsComponent },
  { path: "payments", component: PaymentsComponent },
  { path: "beneficiaries", component: BeneficiariesComponent },
  { path: "loans", component: LoansComponent },
  { path: "cards", component: CardsComponent },
  { path: "**", redirectTo: "accounts" }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
