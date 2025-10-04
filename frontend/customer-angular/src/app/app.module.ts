import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { HttpClientModule, HTTP_INTERCEPTORS } from "@angular/common/http";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { AccountsComponent } from "./pages/accounts/accounts.component";
import { PaymentsComponent } from "./pages/payments/payments.component";
import { BeneficiariesComponent } from "./pages/beneficiaries/beneficiaries.component";
import { LoansComponent } from "./pages/loans/loans.component";
import { CardsComponent } from "./pages/cards/cards.component";
import { CorrelationIdInterceptor } from "./interceptors/correlation-id.interceptor";

@NgModule({
  declarations: [
    AppComponent,
    AccountsComponent,
    PaymentsComponent,
    BeneficiariesComponent,
    LoansComponent,
    CardsComponent
  ],
  imports: [BrowserModule, HttpClientModule, FormsModule, ReactiveFormsModule, AppRoutingModule],
  providers: [{ provide: HTTP_INTERCEPTORS, useClass: CorrelationIdInterceptor, multi: true }],
  bootstrap: [AppComponent]
})
export class AppModule {}
