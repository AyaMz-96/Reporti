import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import { Category, PriorityLevel, ReportResponse, ReportStatus } from './models';
import { readError } from './report-api';
import { ReportEvents } from './report-events';
import { ReportListApi } from './report-list-api';

const PAGE_SIZE = 5;

@Component({
  selector: 'app-report-list',
  imports: [DatePipe],
  templateUrl: './report-list.html',
  styleUrl: './report-list.css',
})
export class ReportList implements OnInit {
  private readonly api = inject(ReportListApi);
  private readonly events = inject(ReportEvents);

  readonly statuses: ReportStatus[] = ['RECEIVED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];
  readonly priorities: PriorityLevel[] = ['URGENT', 'NORMAL', 'LOW'];

  // The app is zoneless, so everything that changes after an async call must be a signal
  readonly reports = signal<ReportResponse[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly search = signal('');
  readonly categoryId = signal<number | null>(null);
  readonly status = signal<ReportStatus | null>(null);
  readonly priority = signal<PriorityLevel | null>(null);
  readonly page = signal(0);

  readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const categoryId = this.categoryId();
    const status = this.status();
    const priority = this.priority();

    return this.reports().filter(
      (report) =>
        (!term ||
          report.title.toLowerCase().includes(term) ||
          report.description.toLowerCase().includes(term)) &&
        (categoryId === null || report.categoryId === categoryId) &&
        (status === null || report.status === status) &&
        (priority === null || report.priority === priority),
    );
  });

  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / PAGE_SIZE)));

  readonly pageItems = computed(() => {
    const start = this.page() * PAGE_SIZE;
    return this.filtered().slice(start, start + PAGE_SIZE);
  });

  constructor() {
    // Reload the list whenever a report is created from the modal
    effect(() => {
      if (this.events.created() > 0) {
        this.api.getReports().subscribe({
          next: (reports) => this.reports.set(reports),
          error: (error) => this.errorMessage.set(`Could not load reports: ${readError(error)}`),
        });
      }
    });
  }

  ngOnInit(): void {
    forkJoin({
      reports: this.api.getReports(),
      categories: this.api.getCategories(),
    }).subscribe({
      next: ({ reports, categories }) => {
        this.reports.set(reports);
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(`Could not load reports: ${readError(error)}`);
        this.loading.set(false);
      },
    });
  }

  categoryName(id: number): string {
    return this.categories().find((category) => category.id === id)?.name ?? `#${id}`;
  }

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
    this.page.set(0);
  }

  onCategory(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.categoryId.set(value === '' ? null : Number(value));
    this.page.set(0);
  }

  onStatus(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.status.set(value === '' ? null : (value as ReportStatus));
    this.page.set(0);
  }

  onPriority(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.priority.set(value === '' ? null : (value as PriorityLevel));
    this.page.set(0);
  }

  reset(): void {
    this.search.set('');
    this.categoryId.set(null);
    this.status.set(null);
    this.priority.set(null);
    this.page.set(0);
  }

  previous(): void {
    this.page.update((page) => Math.max(0, page - 1));
  }

  next(): void {
    this.page.update((page) => Math.min(this.pageCount() - 1, page + 1));
  }
}