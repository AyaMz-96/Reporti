import { Component, DestroyRef, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { forkJoin } from 'rxjs';

import { LoadingSpinner } from '../loading-spinner/loading-spinner';
import { Category, ReportResponse } from '../models';
import { readError } from '../report-api';
import { ReportListApi } from '../report-list-api';

@Component({
  selector: 'app-map-page',
  imports: [LoadingSpinner],
  templateUrl: './map-page.html',
  styleUrl: './map-page.css',
})
export class MapPage implements OnInit {
  private readonly api = inject(ReportListApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly mapElement = viewChild<ElementRef<HTMLDivElement>>('map');
  private map: L.Map | null = null;

  readonly reports = signal<ReportResponse[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    forkJoin({
      reports: this.api.getReports(),
      categories: this.api.getCategories(),
    }).subscribe({
      next: ({ reports, categories }) => {
        this.reports.set(reports);
        this.categories.set(categories);
        this.loading.set(false);
        setTimeout(() => this.initializeMap(), 0);
      },
      error: (error) => {
        this.errorMessage.set(readError(error));
        this.loading.set(false);
      },
    });

    this.destroyRef.onDestroy(() => this.map?.remove());
  }

  categoryName(categoryId: number): string {
    return this.categories().find((category) => category.id === categoryId)?.name ?? 'Community';
  }

  private initializeMap(): void {
    const element = this.mapElement()?.nativeElement;
    if (!element || this.map) {
      return;
    }

    const validReports = this.reports().filter(
      (report) =>
        Number.isFinite(report.latitude) &&
        Number.isFinite(report.longitude) &&
        report.latitude >= -90 &&
        report.latitude <= 90 &&
        report.longitude >= -180 &&
        report.longitude <= 180,
    );

    const initialPosition: L.LatLngExpression =
      validReports.length > 0
        ? [validReports[0].latitude, validReports[0].longitude]
        : [36.8065, 10.1815];

    this.map = L.map(element, {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView(initialPosition, validReports.length > 0 ? 12 : 10);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);

    const bounds: L.LatLngExpression[] = [];

    for (const report of validReports) {
      const position: L.LatLngExpression = [report.latitude, report.longitude];
      bounds.push(position);

      L.circleMarker(position, {
        radius: 8,
        weight: 2,
        color: '#fff',
        fillColor: report.priority === 'URGENT' ? '#d63031' : '#1c1917',
        fillOpacity: 0.95,
      })
        .bindPopup(
          `<article class="report-popup">
            <span class="popup-category">${this.categoryName(report.categoryId)}</span>
            <strong>${this.escapeHtml(report.title)}</strong>
            <p>${this.escapeHtml(report.description)}</p>
          </article>`,
        )
        .addTo(this.map);
    }

    if (bounds.length > 1) {
      this.map.fitBounds(L.latLngBounds(bounds), { padding: [40, 40], maxZoom: 14 });
    }

    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private escapeHtml(value: string): string {
    const element = document.createElement('div');
    element.textContent = value;
    return element.innerHTML;
  }
}
