import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';

import { API_BASE_URL, CITIZEN_ID } from './config';
import { AppNotification } from './models';

// TEMPORARY: set to false once the backend notification endpoints exist
const USE_MOCK = false;

const MOCK_NOTIFICATIONS: AppNotification[] = [
  {
    id: 1,
    message: 'Your report "Broken street light" is now In progress',
    createdAt: '2026-11-02T08:30:00Z',
    isRead: false,
    type: 'STATUS_CHANGED',
    reportId: 1,
  },
  {
    id: 2,
    message: 'An agent replied to your report "Water leak"',
    createdAt: '2026-11-02T07:10:00Z',
    isRead: false,
    type: 'NEW_COMMENT',
    reportId: 2,
  },
  {
    id: 3,
    message: 'New report in your sector: "Pothole on Main Street"',
    createdAt: '2026-11-01T18:45:00Z',
    isRead: false,
    type: 'NEW_REPORT',
    reportId: 3,
  },
  {
    id: 4,
    message: 'A citizen disputed the resolution of report #4',
    createdAt: '2026-11-01T12:00:00Z',
    isRead: true,
    type: 'DISPUTE',
    reportId: 4,
  },
];

@Injectable({ providedIn: 'root' })
export class NotificationApi {
  private readonly http = inject(HttpClient);

  // TEMPORARY: same header as ReportApi until real authentication exists
  private readonly citizenHeaders = new HttpHeaders({ 'X-Citizen-Id': String(CITIZEN_ID) });

  getNotifications(): Observable<AppNotification[]> {
    if (USE_MOCK) {
      return of(MOCK_NOTIFICATIONS);
    }
    return this.http.get<AppNotification[]>(`${API_BASE_URL}/notifications`, {
      headers: this.citizenHeaders,
    });
  }

  markAsRead(id: number): Observable<void> {
    if (USE_MOCK) {
      return of(undefined);
    }
    return this.http.patch<void>(`${API_BASE_URL}/notifications/${id}/read`, null, {
      headers: this.citizenHeaders,
    });
  }

  markAllAsRead(): Observable<void> {
    if (USE_MOCK) {
      return of(undefined);
    }
    return this.http.patch<void>(`${API_BASE_URL}/notifications/read-all`, null, {
      headers: this.citizenHeaders,
    });
  }
}