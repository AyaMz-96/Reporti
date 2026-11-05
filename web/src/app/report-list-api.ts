import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';

import { API_BASE_URL, CITIZEN_ID } from './config';
import { Category, PriorityLevel, ReportResponse, ReportStatus } from './models';

// TEMPORARY: set to false once the backend list endpoint exists
const USE_MOCK = false;

const MOCK_CATEGORIES: Category[] = [
  { id: 1, name: 'Water', description: null },
  { id: 2, name: 'Electricity', description: null },
  { id: 3, name: 'Road', description: null },
  { id: 4, name: 'Lighting', description: null },
];

function mockReport(
  id: number,
  title: string,
  categoryId: number,
  status: ReportStatus,
  priority: PriorityLevel,
  createdAt: string,
): ReportResponse {
  return {
    id,
    title,
    description: `Description of: ${title}`,
    status,
    priority,
    latitude: 36.8065,
    longitude: 10.1815,
    categoryId,
    sectorId: 1,
    createdAt,
    mediaUrls: [],
  };
}

const MOCK_REPORTS: ReportResponse[] = [
  mockReport(1, 'Broken street light', 4, 'IN_PROGRESS', 'NORMAL', '2026-11-02T08:30:00Z'),
  mockReport(2, 'Water leak near the school', 1, 'RECEIVED', 'URGENT', '2026-11-02T07:10:00Z'),
  mockReport(3, 'Pothole on Main Street', 3, 'RECEIVED', 'NORMAL', '2026-11-01T18:45:00Z'),
  mockReport(4, 'Power cut in the market', 2, 'RESOLVED', 'URGENT', '2026-11-01T12:00:00Z'),
  mockReport(5, 'Flickering lamp in the park', 4, 'RESOLVED', 'LOW', '2026-10-31T20:15:00Z'),
  mockReport(6, 'Road damaged after rain', 3, 'REJECTED', 'LOW', '2026-10-30T09:00:00Z'),
  mockReport(7, 'No water in the building', 1, 'IN_PROGRESS', 'URGENT', '2026-10-29T14:20:00Z'),
];

@Injectable({ providedIn: 'root' })
export class ReportListApi {
  private readonly http = inject(HttpClient);

  // TEMPORARY: same header as ReportApi until real authentication exists
  private readonly citizenHeaders = new HttpHeaders({ 'X-Citizen-Id': String(CITIZEN_ID) });

  getReports(): Observable<ReportResponse[]> {
    if (USE_MOCK) {
      return of(MOCK_REPORTS);
    }
    return this.http.get<ReportResponse[]>(`${API_BASE_URL}/reports`, {
      headers: this.citizenHeaders,
    });
  }

  getCategories(): Observable<Category[]> {
    if (USE_MOCK) {
      return of(MOCK_CATEGORIES);
    }
    return this.http.get<Category[]>(`${API_BASE_URL}/categories`);
  }
}