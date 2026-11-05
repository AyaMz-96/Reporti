import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL, CITIZEN_ID } from './config';
import { Category, CreateReportRequest, ReportResponse, Sector } from './models';

/** Turns any failed request into a message that is safe to show the user. */
export function readError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    // The backend answers errors as {"message": "..."}
    const message = error.error?.message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
    return `Request failed (${error.status})`;
  }
  return 'Something went wrong';
}

@Injectable({ providedIn: 'root' })
export class ReportApi {
  private readonly http = inject(HttpClient);

  // TEMPORARY: the backend identifies the citizen from this header until real authentication exists
  private readonly citizenHeaders = new HttpHeaders({ 'X-Citizen-Id': String(CITIZEN_ID) });

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${API_BASE_URL}/categories`);
  }

  getSectors(): Observable<Sector[]> {
    return this.http.get<Sector[]>(`${API_BASE_URL}/sectors`);
  }

  createReport(request: CreateReportRequest): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${API_BASE_URL}/reports`, request, {
      headers: this.citizenHeaders,
    });
  }

  uploadMedia(reportId: number, files: File[]): Observable<string[]> {
    const body = new FormData();
    // Same field name as @RequestParam("files") on the backend; repeated once per file
    files.forEach((file) => body.append('files', file));
    // No Content-Type header on purpose: the browser must set it itself to include the multipart boundary
    return this.http.post<string[]>(`${API_BASE_URL}/reports/${reportId}/media`, body, {
      headers: this.citizenHeaders,
    });
  }
}