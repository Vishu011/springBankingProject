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
    const secureCfg = (environment as any).secure as { useAuthToken?: boolean; bearerToken?: string } | undefined;
    if (!secureCfg || !secureCfg.useAuthToken || !secureCfg.bearerToken) {
      return next.handle(req);
    }
    // Attach only if not already present
    if (req.headers.has("Authorization")) {
      return next.handle(req);
    }
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${secureCfg.bearerToken}`
      }
    });
    return next.handle(cloned);
  }
}
