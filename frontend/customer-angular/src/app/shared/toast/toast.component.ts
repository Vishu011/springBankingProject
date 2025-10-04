import { Component } from "@angular/core";
import { Observable } from "rxjs";
import { ToastMessage, ToastService } from "../../services/toast.service";

@Component({
  selector: "app-toast-container",
  templateUrl: "./toast.component.html",
  styleUrls: ["./toast.component.css"]
})
export class ToastContainerComponent {
  toasts$: Observable<ToastMessage[]>;

  constructor(private toast: ToastService) {
    this.toasts$ = this.toast.toasts$;
  }

  remove(id: string) {
    this.toast.remove(id);
  }

  copy(text: string | undefined) {
    if (!text) return;
    try {
      navigator.clipboard.writeText(text);
    } catch {}
  }

  trackById(_index: number, item: ToastMessage) {
    return item.id;
  }
}
