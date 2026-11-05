import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';

import { AppNotification, NotificationType } from './models';
import { NotificationApi } from './notification-api';
import { readError } from './report-api';

// How often the list is refreshed, so new notifications show up without reloading the page
const REFRESH_INTERVAL_MS = 30000;

const TYPE_LABELS: Record<NotificationType, string> = {
  STATUS_CHANGED: 'Status update',
  NEW_COMMENT: 'New comment',
  ASSIGNED: 'New assignment',
  URGENT_UPDATE: 'Urgent',
  NEW_REPORT: 'New report',
  DISPUTE: 'Dispute',
  AGENT_REFUSAL: 'Agent refusal',
  SYSTEM_ALERT: 'System alert',
};

@Component({
  selector: 'app-notification-bell',
  imports: [DatePipe],
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.css',
})
export class NotificationBell implements OnInit {
  private readonly api = inject(NotificationApi);
  private readonly destroyRef = inject(DestroyRef);

  // The app is zoneless, so everything that changes after an async call must be a signal
  readonly notifications = signal<AppNotification[]>([]);
  readonly open = signal(false);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly unreadCount = computed(
    () => this.notifications().filter((notification) => !notification.isRead).length,
  );

  ngOnInit(): void {
    this.load();
    const timer = setInterval(() => this.load(), REFRESH_INTERVAL_MS);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }

  toggle(): void {
    this.open.update((value) => !value);
  }

  label(notification: AppNotification): string {
    // The type column does not exist in the database yet, so it can be missing
    return notification.type ? TYPE_LABELS[notification.type] : 'Notification';
  }

  markAsRead(notification: AppNotification): void {
    if (notification.isRead) {
      return;
    }
    this.api.markAsRead(notification.id).subscribe({
      next: () =>
        this.notifications.update((list) =>
          list.map((item) => (item.id === notification.id ? { ...item, isRead: true } : item)),
        ),
      error: (error) => this.errorMessage.set(readError(error)),
    });
  }

  markAllAsRead(): void {
    this.api.markAllAsRead().subscribe({
      next: () =>
        this.notifications.update((list) => list.map((item) => ({ ...item, isRead: true }))),
      error: (error) => this.errorMessage.set(readError(error)),
    });
  }

  private load(): void {
    this.api.getNotifications().subscribe({
      next: (list) => {
        this.notifications.set(list);
        this.errorMessage.set(null);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(readError(error));
        this.loading.set(false);
      },
    });
  }
}