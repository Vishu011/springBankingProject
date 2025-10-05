import { Component } from "@angular/core";
import { Observable } from "rxjs";
import { LoadingService } from "../../services/loading.service";

@Component({
  selector: "app-loading-overlay",
  templateUrl: "./loading.component.html",
  styleUrls: ["./loading.component.css"]
})
export class LoadingOverlayComponent {
  isLoading$: Observable<boolean>;

  constructor(private loading: LoadingService) {
    this.isLoading$ = this.loading.isLoading$;
  }
}
