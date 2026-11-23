import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/** Marks state-changing API calls as coming from the SPA (CSRF protection, checked by pta-www). */
export const ptaRequestHeader: HttpInterceptorFn = (request, next) =>
  request.method === 'GET' || !request.url.startsWith('/api/')
    ? next(request)
    : next(request.clone({ setHeaders: { 'X-PTA-Request': '1' } }));

export interface BookingResult {
  session: string;
  status: 'DONE' | 'PREBOOKED' | 'WAITING';
}

/** Books and cancels on behalf of the signed-in participant (identified by the server, never by the SPA). */
@Injectable({ providedIn: 'root' })
export class BookingActions {
  private readonly http = inject(HttpClient);

  book(session: string): Promise<BookingResult> {
    return firstValueFrom(this.http.post<BookingResult>(`/api/sessions/${session}/booking`, null));
  }

  cancel(session: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/sessions/${session}/booking`));
  }
}

/** The problem detail sent by pta-www, or a generic message. */
export function problemDetail(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401) return 'Your session has expired. Please sign in again.';
    const detail = (error.error as { detail?: unknown } | null)?.detail;
    if (typeof detail === 'string' && detail) return detail;
  }
  return 'Something went wrong. Please try again.';
}
