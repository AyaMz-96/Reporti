import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './config';
import { AuthApi } from './auth/auth-api';
import { ReportResponse } from './models';

@Injectable({ providedIn: 'root' })
export class AgentApi {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthApi);

  /** Admin flow: available agents matching a report's category & sector. */
  getAvailableAgents(categoryId?: number | null, sectorId?: number | null): Observable<any[]> {
    let params = new HttpParams();
    if (categoryId != null) params = params.set('categoryId', String(categoryId));
    if (sectorId != null) params = params.set('sectorId', String(sectorId));
    return this.http.get<any[]>(`${API_BASE_URL}/agents/available`, { headers: this.authHeaders(), params });
  }

  /** Admin flow: assign an arbitrary agent to a report. */
  assignReport(reportId: number, agentId: string): Observable<any> {
    return this.http.patch<any>(`${API_BASE_URL}/reports/${reportId}/assign`, { agentId }, { headers: this.authHeaders() });
  }

  /** Agent flow: reports I'm fit for and can claim right now. */
  getMyAvailable(): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${API_BASE_URL}/agents/me/available`, { headers: this.authHeaders() });
  }

  /** Agent flow: reports already assigned to me. */
  getMyActive(): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${API_BASE_URL}/agents/me/active`, { headers: this.authHeaders() });
  }

  /** Agent flow: claim a report for myself. */
  claimReport(reportId: number): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${API_BASE_URL}/agents/me/claim/${reportId}`, {}, { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${this.auth.token() ?? ''}`,
    });
  }
}
