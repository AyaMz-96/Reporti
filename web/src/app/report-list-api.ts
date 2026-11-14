import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL, CITIZEN_ID } from './config';
import { Category, ReportResponse, Sector } from './models';

@Injectable({ providedIn: 'root' })
export class ReportListApi {
  private readonly http = inject(HttpClient);
  private readonly citizenHeaders = new HttpHeaders({ 'X-Citizen-Id': String(CITIZEN_ID) });

  getReports(): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${API_BASE_URL}/reports`, { headers: this.citizenHeaders });
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${API_BASE_URL}/categories`);
  }

  getSectors(): Observable<Sector[]> {
    return this.http.get<Sector[]>(`${API_BASE_URL}/sectors`);
  }
}
