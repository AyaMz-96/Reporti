import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './config';
import { AuthApi } from './auth/auth-api';

@Injectable({ providedIn: 'root' })
export class AgentApi {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthApi);

  getAvailableAgents(categoryId?: number | null, sectorId?: number | null): Observable<any[]> {
    let params = new HttpParams();
    if (categoryId != null) params = params.set('categoryId', String(categoryId));
    if (sectorId != null) params = params.set('sectorId', String(sectorId));
    return this.http.get<any[]>(`${API_BASE_URL}/agents/available`, { headers: this.getHeaders(), params });
  }

  assignReport(reportId: number, agentId: string): Observable<any> {
    return this.http.patch<any>(`${API_BASE_URL}/reports/${reportId}/assign`, { agentId }, { headers: this.getHeaders() });
  }

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${this.auth.token() ?? ''}`
    });
  }
}
