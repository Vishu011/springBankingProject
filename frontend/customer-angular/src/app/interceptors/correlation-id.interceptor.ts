import { Injectable } from "@angular/core";
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from "@angular/common/http";
import { Observable, tap } from "rxjs";
import { environment } from "../../environments/environment";

function guid(): string {
  // lightweight GUID
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
    const r = (Math.random() * 16) | 0, v = c == "x" ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

@Injectable()
export class CorrelationIdInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const cidHdr = environment.xCorrelationIdHeader || "X-Correlation-Id";
    const cid = guid();
    const cloned = req.clone({ setHeaders: { [cidHdr]: cid } });
    return next.handle(cloned).pipe(
      tap(evt => {
        if (evt instanceof HttpResponse) {
          const respCid = evt.headers.get(cidHdr);
          if (respCid) {
            // Could surface in a toast or console for support correlation
            // console.log("CorrelationId:", respCid);
          }
        }
      })
    );
  }
}
