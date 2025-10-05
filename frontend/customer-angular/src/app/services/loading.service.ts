import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";

/**
 * Tracks in-flight HTTP requests to drive a global loading indicator.
 * Increments on request, decrements on response/finalize.
 */
@Injectable({ providedIn: "root" })
export class LoadingService {
  private pending = 0;
  private readonly subject = new BehaviorSubject<boolean>(false);
  readonly isLoading$: Observable<boolean> = this.subject.asObservable();

  start(): void {
    this.pending++;
    if (this.pending === 1) {
      this.subject.next(true);
    }
  }

  stop(): void {
    if (this.pending > 0) {
      this.pending--;
      if (this.pending === 0) {
        this.subject.next(false);
      }
    }
  }

  reset(): void {
    this.pending = 0;
    this.subject.next(false);
  }
}
