import { DatePipe } from '@angular/common';
import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Category, ReportResponse } from '../models';
import { readError } from '../report-api';
import { ReportListApi } from '../report-list-api';
import { LoadingSpinner } from '../loading-spinner/loading-spinner';

@Component({
  selector: 'app-home',
  imports: [DatePipe, LoadingSpinner, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly api = inject(ReportListApi);
  private readonly carousel = viewChild<ElementRef<HTMLDivElement>>('reportCarousel');

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
        this.reports.set(reports.slice(0, 10));
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readError(error));
        this.loading.set(false);
      },
    });
  }

  categoryName(categoryId: number): string {
    return this.categories().find((category) => category.id === categoryId)?.name ?? 'Community';
  }

  statusLabel(status: string): string {
    return status.replace('_', ' ');
  }

  scroll(direction: -1 | 1): void {
    const element = this.carousel()?.nativeElement;
    if (!element) {
      return;
    }

    element.scrollBy({
      left: direction * Math.min(element.clientWidth * 0.82, 380),
      behavior: 'smooth',
    });
  }
}
