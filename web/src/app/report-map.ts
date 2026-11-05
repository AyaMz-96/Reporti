import {
  Component,
  DestroyRef,
  OnInit,
  afterNextRender,
  effect,
  inject,
  signal,
  viewChild,
  ElementRef,
} from '@angular/core';
import * as L from 'leaflet';
import { forkJoin } from 'rxjs';

import { PriorityLevel, Category, ReportResponse, ReportStatus } from './models';
import { readError } from './report-api';
import { ReportEvents } from './report-events';
import { ReportListApi } from './report-list-api';

// Centre of Tunisia, used until there are reports to fit the view around
const TUNISIA_CENTER: L.LatLngTuple = [33.9, 9.5];
const TUNISIA_ZOOM = 6;

export const STATUS_COLORS: Record<ReportStatus, string> = {
  RECEIVED: '#2563eb',
  IN_PROGRESS: '#f59e0b',
  RESOLVED: '#16a34a',
  REJECTED: '#6b7280',
};
export const PRIORITY_STYLES: Record<PriorityLevel, { color: string; weight: number; radius: number }> = {
  LOW: { color: '#ffffff', weight: 2, radius: 8 },
  NORMAL: { color: '#facc15', weight: 3, radius: 8 },
  URGENT: { color: '#dc2626', weight: 4, radius: 12 },
};

@Component({
  selector: 'app-report-map',
  imports: [],
  templateUrl: './report-map.html',
  styleUrl: './report-map.css',
})
export class ReportMap implements OnInit {
  private readonly api = inject(ReportListApi);
  private readonly events = inject(ReportEvents);
  private readonly destroyRef = inject(DestroyRef);
  private readonly container = viewChild.required<ElementRef<HTMLDivElement>>('mapContainer');

  private map: L.Map | null = null;
  private markers = L.layerGroup();

  // The app is zoneless, so everything that changes after an async call must be a signal
  readonly reports = signal<ReportResponse[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly mapReady = signal(false);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly legend = Object.entries(STATUS_COLORS).map(([status, color]) => ({
    label: status.replace('_', ' '),
    color,
    
  }));
  readonly priorityLegend = [
    { label: 'Low', color: '#cbd5e1' },
    { label: 'Normal', color: '#facc15' },
    { label: 'Urgent', color: '#dc2626' },
  ];
  constructor() {
    afterNextRender(() => {
      this.map = L.map(this.container().nativeElement).setView(TUNISIA_CENTER, TUNISIA_ZOOM);
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      }).addTo(this.map);
      this.markers.addTo(this.map);
      this.mapReady.set(true);
    });

    // Redraw the markers whenever the reports change (or the map finishes initialising)
    effect(() => {
      const reports = this.reports();
      if (this.mapReady()) {
        this.drawMarkers(reports);
      }
    });

    // Reload when a report is created from the modal
    effect(() => {
      if (this.events.created() > 0) {
        this.api.getReports().subscribe({
          next: (reports) => this.reports.set(reports),
          error: (error) => this.errorMessage.set(`Could not load reports: ${readError(error)}`),
        });
      }
    });

    this.destroyRef.onDestroy(() => this.map?.remove());
  }

  ngOnInit(): void {
    forkJoin({
      reports: this.api.getReports(),
      categories: this.api.getCategories(),
    }).subscribe({
      next: ({ reports, categories }) => {
        this.categories.set(categories);
        this.reports.set(reports);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(`Could not load reports: ${readError(error)}`);
        this.loading.set(false);
      },
    });
  }

  private drawMarkers(reports: ReportResponse[]): void {
    if (!this.map) {
      return;
    }
    this.markers.clearLayers();

    const points: L.LatLngTuple[] = [];
    for (const report of reports) {
      // Skip rows without usable coordinates instead of crashing the whole map
      if (report.latitude == null || report.longitude == null) {
        continue;
      }
      const point: L.LatLngTuple = [report.latitude, report.longitude];
      points.push(point);

      const style = PRIORITY_STYLES[report.priority] ?? PRIORITY_STYLES.NORMAL;
      const marker = L.circleMarker(point, {
        radius: style.radius,
        color: style.color,
        weight: style.weight,
        fillColor: STATUS_COLORS[report.status] ?? '#6b7280',
        fillOpacity: 0.95,
      })
        .bindPopup(this.popupContent(report))
        .addTo(this.markers);

      // Keep urgent dots on top so a crowd of normal ones never hides them
      if (report.priority === 'URGENT') {
        marker.bringToFront();
      }
    if (points.length > 0) {
      this.map.fitBounds(L.latLngBounds(points), { padding: [40, 40], maxZoom: 15 });
    }
  }}

  /** Built with DOM nodes and textContent so report text is never interpreted as HTML. */
  private popupContent(report: ReportResponse): HTMLElement {
  const category =
      this.categories().find((item) => item.id === report.categoryId)?.name ?? `#${report.categoryId}`;

    const root = document.createElement('div');
    const title = document.createElement('strong');
    title.textContent = report.title;
    const meta = document.createElement('div');
    meta.textContent = `${category} · ${report.status.replace('_', ' ')} · ${report.priority}`;
    const date = document.createElement('small');
    date.textContent = new Date(report.createdAt).toLocaleString();

    root.append(title, meta, date);
    return root;
  }
}
