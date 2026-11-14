import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './config';
import { AuthApi } from './auth/auth-api';
import { AppNotification, ReportResponse, ReportStatus } from './models';

export interface ReportComment {
  id: number;
  reportId: number;
  authorId: string;
  authorName: string | null;
  content: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AgentWorkApi {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthApi);
  private readonly base = `${API_BASE_URL}/agents/me`;

  // T24 : notifications de l'agent
  getNotifications(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(`${this.base}/notifications`, { headers: this.headers() });
  }
  markNotificationRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/notifications/${id}/read`, null, { headers: this.headers() });
  }
  markAllNotificationsRead(): Observable<void> {
    return this.http.patch<void>(`${this.base}/notifications/read-all`, null, { headers: this.headers() });
  }

  // T25 : signalements assignés (filtre optionnel sur le statut)
  getAssigned(status?: ReportStatus | ''): Observable<ReportResponse[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<ReportResponse[]>(`${this.base}/assigned`, { headers: this.headers(), params });
  }

  // T26 : changer le statut
  changeStatus(reportId: number, status: 'IN_PROGRESS' | 'RESOLVED'): Observable<ReportResponse> {
    return this.http.patch<ReportResponse>(
      `${this.base}/reports/${reportId}/status`, { status }, { headers: this.headers() });
  }

  // T27 : commentaires de traitement
  getComments(reportId: number): Observable<ReportComment[]> {
    return this.http.get<ReportComment[]>(`${this.base}/reports/${reportId}/comments`, { headers: this.headers() });
  }
  addComment(reportId: number, content: string): Observable<ReportComment> {
    return this.http.post<ReportComment>(
      `${this.base}/reports/${reportId}/comments`, { content }, { headers: this.headers() });
  }

  // T28 : disponibilité
  getAvailability(): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(`${this.base}/availability`, { headers: this.headers() });
  }
  setAvailability(available: boolean): Observable<{ available: boolean }> {
    return this.http.patch<{ available: boolean }>(
      `${this.base}/availability`, { available }, { headers: this.headers() });
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${this.auth.token() ?? ''}`,
    });
  }
}