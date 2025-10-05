import { Injectable } from "@angular/core";
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";

/**
 * Optional Authorization bearer token interceptor, guarded by environment flag.
 * Defaults to NO-OP unless environment.secure.useAuthToken is true and a bearerToken is provided.
 */
@Injectable()
export class AuthTokenInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Source 1: environment.secure (compile-time)
    let useAuthToken: boolean | undefined;
    let bearerToken: string | undefined;
    const envSecure = (environment as any).secure as { useAuthToken?: boolean; bearerToken?: string } | undefined;
    if (envSecure) {
      useAuthToken = envSecure.useAuthToken;
      bearerToken = envSecure.bearerToken;
    }
    // Source 2: localStorage (runtime toggle from Settings page)
    try {
      const lsUse = localStorage.getItem("secure.useAuthToken");
      const lsToken = localStorage.getItem("secure.bearerToken");
      if (lsUse !== null) useAuthToken = lsUse === "true";
      if (lsToken !== null && lsToken.trim().length > 0) bearerToken = lsToken;
    } catch {}
    if (!useAuthToken || !bearerToken) {
      return next.handle(req);
    }
    // Attach only if not already present
    if (req.headers.has("Authorization")) {
      return next.handle(req);
    }
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${bearerToken}`
      }
    });
    return next.handle(cloned);
  }
}
