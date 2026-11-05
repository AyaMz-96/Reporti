import { Injectable, signal } from '@angular/core';

/** Lets the report form tell the list (and later the map) that a new report exists, so they reload. */
@Injectable({ providedIn: 'root' })
export class ReportEvents {
  /** Incremented every time a report is created. */
  readonly created = signal(0);

  notifyCreated(): void {
    this.created.update((count) => count + 1);
  }
}
