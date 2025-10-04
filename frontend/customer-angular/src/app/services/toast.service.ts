import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

export type ToastType = "info" | "success" | "error";

export interface ToastMessage {
  id: string;
  type: ToastType;
  message: string;
  correlationId?: string;
  createdAt: number;
}

function randomId(): string {
  return "t-" + Math.random().toString(36).slice(2);
}

@Injectable({ providedIn: "root" })
export class ToastService {
  private readonly toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  readonly toasts$ = this.toastsSubject.asObservable();

  show(message: string, type: ToastType = "info", correlationId?: string, durationMs = 5000): void {
    const toast: ToastMessage = {
      id: randomId(),
      type,
      message,
      correlationId,
      createdAt: Date.now()
    };
    const arr = [...this.toastsSubject.value, toast];
    this.toastsSubject.next(arr);

    if (durationMs > 0) {
      setTimeout(() => this.remove(toast.id), durationMs);
    }
  }

  success(message: string, durationMs = 4000): void {
    this.show(message, "success", undefined, durationMs);
  }

  error(message: string, correlationId?: string, durationMs = 7000): void {
    this.show(message, "error", correlationId, durationMs);
  }

  info(message: string, durationMs = 4000): void {
    this.show(message, "info", undefined, durationMs);
  }

  remove(id: string): void {
    const arr = this.toastsSubject.value.filter(t => t.id !== id);
    this.toastsSubject.next(arr);
  }

  clear(): void {
    this.toastsSubject.next([]);
  }
}
