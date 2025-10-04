import { Injectable } from "@angular/core";
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from "@angular/common/http";
import { Observable, catchError, throwError } from "rxjs";
import { ToastService } from "../services/toast.service";

@Injectable()
export class ErrorHandlerInterceptor implements HttpInterceptor {
  constructor(private toast: ToastService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err: any) => {
        if (err instanceof HttpErrorResponse) {
          const hdrCid = err.headers?.get("X-Correlation-Id") || undefined;

          // Standard error envelope (as per backend): { timestamp, status, error, message, correlationId, path }
          const body = (typeof err.error === "object" && err.error) ? err.error : undefined;

          const correlationId: string | undefined =
            (body && typeof body.correlationId === "string" && body.correlationId) || hdrCid;

          let message = "Unexpected error";
          if (body && typeof body.message === "string" && body.message) {
            message = body.message;
          } else if (typeof err.message === "string" && err.message) {
            message = err.message;
          } else if (err.status) {
            message = `HTTP ${err.status}`;
          }

          // Surface toasts with correlationId for quick supportability
          this.toast.error(message, correlationId);

          // Re-throw so callers can handle if needed
          return throwError(() => err);
        }

        this.toast.error("Network error");
        return throwError(() => err);
      })
    );
  }
}
