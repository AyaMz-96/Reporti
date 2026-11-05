import { Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { NotificationBell } from './notification-bell';
import { ReportEvents } from './report-events';
import { ReportForm } from './report-form';

@Component({
  selector: 'app-root',
  imports: [NotificationBell, ReportForm, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly events = inject(ReportEvents);

  readonly formOpen = signal(false);
  readonly toast = signal<string | null>(null);

  openForm(): void {
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeForm();
  }

  onSubmitted(reportId: number): void {
    this.closeForm();
    this.events.notifyCreated();
    this.toast.set(`Report #${reportId} submitted. Thank you!`);
    setTimeout(() => this.toast.set(null), 4000);
  }
}
