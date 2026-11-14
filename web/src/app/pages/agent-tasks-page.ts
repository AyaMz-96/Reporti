import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AgentWorkApi, ReportComment } from '../agent-work-api';
import { ReportListApi } from '../report-list-api';
import { AppNotification, ReportResponse, ReportStatus } from '../models';
import { readError } from '../report-api';
import { supabase } from '../auth/auth-api';

@Component({
  selector: 'app-agent-tasks-page',
  imports: [DatePipe],
  template: `
    <main class="wrap">
      <header class="top">
        <div>
          <h1>My work</h1>
          <p class="muted">Reports assigned to you, status and treatment notes</p>
        </div>
        <!-- T28 -->
        <button type="button" class="avail" [class.on]="available()"
                [disabled]="availabilityBusy() || available() === null" (click)="toggleAvailability()">
          <span class="dot"></span>
          {{ available() === null ? '…' : available() ? 'Available' : 'Unavailable' }}
        </button>
      </header>

      @if (error()) { <p class="err">{{ error() }}</p> }
      @if (info()) { <p class="ok">{{ info() }}</p> }

      <!-- T24 : notifications -->
      <section class="card">
        <div class="row">
          <h2>Notifications @if (unreadCount() > 0) { <span class="count">{{ unreadCount() }}</span> }</h2>
          <button type="button" (click)="markAllRead()" [disabled]="unreadCount() === 0">Mark all as read</button>
        </div>
        @if (notifications().length === 0) {
          <p class="muted" style="margin-top: 1rem;">No notifications yet.</p>
        } @else {
          <ul class="list">
            @for (n of notifications(); track n.id) {
              <li [class.unread]="!n.isRead" (click)="markRead(n)">
                <strong>{{ n.message }}</strong>
                <div class="small">{{ n.createdAt | date: 'short' }}</div>
              </li>
            }
          </ul>
        }
      </section>

      <!-- T25 + T26 + T27 -->
      <section class="card">
        <div class="row">
          <h2>Assigned to me</h2>
          <div class="btns">
            <button type="button" [class.sel]="filter() === ''" (click)="setFilter('')">All</button>
            <button type="button" [class.sel]="filter() === 'IN_PROGRESS'" (click)="setFilter('IN_PROGRESS')">In progress</button>
            <button type="button" [class.sel]="filter() === 'RESOLVED'" (click)="setFilter('RESOLVED')">Resolved</button>
          </div>
        </div>

        @if (loading()) {
          <p class="muted" style="margin-top: 1rem;">Loading…</p>
        } @else if (reports().length === 0) {
          <p class="muted" style="margin-top: 1rem;">No report to show.</p>
        } @else {
          <ul class="list">
            @for (r of reports(); track r.id) {
              <li>
                <div class="report-head">
                  <div>
                    <strong>#{{ r.id }} · {{ r.title }}</strong>
                    <div class="small">
                      {{ categoryName(r.categoryId) }} · {{ sectorName(r.sectorId) }} · {{ r.createdAt | date: 'short' }}
                      · <span class="badge">{{ r.status.replace('_', ' ') }}</span>
                    </div>
                  </div>
                  <div class="btns">
                    <button type="button"
                            [disabled]="busyId() === r.id || r.status === 'IN_PROGRESS' || r.status === 'REJECTED'"
                            (click)="setStatus(r, 'IN_PROGRESS')">In progress</button>
                    <button type="button" class="primary"
                            [disabled]="busyId() === r.id || r.status === 'RESOLVED' || r.status === 'REJECTED'"
                            (click)="setStatus(r, 'RESOLVED')">Resolved</button>
                    <button type="button" (click)="toggleComments(r)">{{ openId() === r.id ? 'Hide notes' : 'Notes' }}</button>
                  </div>
                </div>

                @if (openId() === r.id) {
                  <div class="comments">
                    @if (commentsLoading()) {
                      <p class="muted">Loading notes…</p>
                    } @else {
                      @for (c of comments(); track c.id) {
                        <div class="comment">
                          <div class="small">{{ c.authorName || 'Agent' }} · {{ c.createdAt | date: 'short' }}</div>
                          <div>{{ c.content }}</div>
                        </div>
                      } @empty {
                        <p class="muted">No treatment note yet.</p>
                      }
                    }
                    <textarea rows="3" maxlength="2000" placeholder="Add a treatment note…"
                              [value]="draft()" (input)="draft.set($any($event.target).value)"></textarea>
                    <div class="actions">
                      <button type="button" class="primary" [disabled]="commentBusy() || !draft().trim()"
                              (click)="sendComment(r)">{{ commentBusy() ? 'Sending…' : 'Add note' }}</button>
                    </div>
                  </div>
                }
              </li>
            }
          </ul>
        }
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; color: var(--text-main); }
    .wrap { max-width: 1100px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; }
    .top, .row { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
    h1, h2 { margin: 0; color: var(--text-main); }
    .muted { color: var(--muted); margin: .25rem 0 0; }
    .small { font-size: .82rem; color: var(--muted); }
    .card { background: var(--surface-solid); border: 1px solid var(--border); border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); }
    button { padding: .5rem 1rem; border: 1px solid var(--border); border-radius: 999px; background: var(--surface-solid); color: var(--text-main); font: inherit; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .45; cursor: default; }
    .primary, .sel { background: var(--accent-red); color: #fff; border-color: var(--accent-red); }
    .avail { display: inline-flex; align-items: center; gap: .5rem; }
    .dot { width: .7rem; height: .7rem; border-radius: 50%; background: #9ca3af; }
    .avail.on .dot { background: #22c55e; }
    .count { display: inline-block; margin-left: .4rem; padding: 0 .55rem; border-radius: 999px; background: var(--accent-red); color: #fff; font-size: .8rem; }
    .list { list-style: none; padding: 0; margin: 1rem 0 0; display: grid; gap: .75rem; }
    .list li { padding: 1rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .list li.unread { border-color: var(--accent-red); cursor: pointer; }
    .report-head { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; }
    .btns { display: flex; gap: .5rem; flex-wrap: wrap; }
    .badge { display: inline-block; padding: .15rem .6rem; border-radius: 999px; border: 1px solid var(--border); font-size: .78rem; font-weight: 700; }
    .comments { margin-top: 1rem; display: grid; gap: .6rem; }
    .comment { padding: .6rem .8rem; border-radius: 10px; background: var(--surface-solid); border: 1px solid var(--border); }
    textarea { width: 100%; box-sizing: border-box; padding: .6rem; border-radius: 10px; border: 1px solid var(--border); background: var(--surface-solid); color: var(--text-main); font: inherit; resize: vertical; }
    .actions { display: flex; justify-content: flex-end; }
    .err { color: var(--accent-red); margin: 0; }
    .ok { color: #22c55e; margin: 0; }
  `],
})
export class AgentTasksPage implements OnInit {
  private readonly workApi = inject(AgentWorkApi);
  private readonly reportApi = inject(ReportListApi);

  protected readonly reports = signal<ReportResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly filter = signal<ReportStatus | ''>('');
  protected readonly busyId = signal<number | null>(null);

  protected readonly notifications = signal<AppNotification[]>([]);
  protected readonly available = signal<boolean | null>(null);
  protected readonly availabilityBusy = signal(false);

  protected readonly openId = signal<number | null>(null);
  protected readonly comments = signal<ReportComment[]>([]);
  protected readonly commentsLoading = signal(false);
  protected readonly commentBusy = signal(false);
  protected readonly draft = signal('');

  protected readonly categories = signal<any[]>([]);
  protected readonly sectors = signal<any[]>([]);
  protected readonly error = signal('');
  protected readonly info = signal('');

  ngOnInit(): void {
    this.reportApi.getCategories().subscribe((c) => this.categories.set(c));
    this.reportApi.getSectors().subscribe((s) => this.sectors.set(s));
    this.loadReports();
    this.loadNotifications();
    this.workApi.getAvailability().subscribe({
      next: (a) => this.available.set(a.available),
      error: (e) => this.error.set(readError(e)),
    });
  }

  protected categoryName(id: number): string { return this.categories().find((c) => c.id === id)?.name ?? `#${id}`; }
  protected sectorName(id: number): string { return this.sectors().find((s) => s.id === id)?.name ?? `#${id}`; }
  protected unreadCount(): number { return this.notifications().filter((n) => !n.isRead).length; }

  // ----- T25 -----
  private loadReports(): void {
    this.loading.set(true);
    this.workApi.getAssigned(this.filter()).subscribe({
      next: (list) => { this.reports.set(list); this.loading.set(false); },
      error: (e) => { this.error.set(readError(e)); this.loading.set(false); },
    });
  }
  protected setFilter(status: ReportStatus | ''): void {
    this.filter.set(status);
    this.openId.set(null);
    this.loadReports();
  }

  // ----- T24 -----
  private loadNotifications(): void {
    this.workApi.getNotifications().subscribe({
      next: (list) => this.notifications.set(list),
      error: (e) => this.error.set(readError(e)),
    });
  }
  protected markRead(n: AppNotification): void {
    if (n.isRead) return;
    this.workApi.markNotificationRead(n.id).subscribe({
      next: () => this.notifications.update((l) => l.map((x) => (x.id === n.id ? { ...x, isRead: true } : x))),
      error: (e) => this.error.set(readError(e)),
    });
  }
  protected markAllRead(): void {
    this.workApi.markAllNotificationsRead().subscribe({
      next: () => this.notifications.update((l) => l.map((x) => ({ ...x, isRead: true }))),
      error: (e) => this.error.set(readError(e)),
    });
  }

  // ----- T26 -----
  protected setStatus(report: ReportResponse, status: 'IN_PROGRESS' | 'RESOLVED'): void {
    this.busyId.set(report.id);
    this.error.set('');
    this.workApi.changeStatus(report.id, status).subscribe({
      next: () => {
        this.info.set(`Report #${report.id} is now ${status.replace('_', ' ')}.`);
        this.busyId.set(null);
        this.loadReports(); // recharge pour respecter le filtre actif
      },
      error: (e) => { this.error.set(readError(e)); this.busyId.set(null); },
    });
  }

  // ----- T27 -----
  protected toggleComments(report: ReportResponse): void {
    if (this.openId() === report.id) { this.openId.set(null); return; }
    this.openId.set(report.id);
    this.comments.set([]);
    this.draft.set('');
    this.commentsLoading.set(true);
    this.workApi.getComments(report.id).subscribe({
      next: (list) => { this.comments.set(list); this.commentsLoading.set(false); },
      error: (e) => { this.error.set(readError(e)); this.commentsLoading.set(false); },
    });
  }
  protected sendComment(report: ReportResponse): void {
    const content = this.draft().trim();
    if (!content) return;
    this.commentBusy.set(true);
    this.workApi.addComment(report.id, content).subscribe({
      next: (c) => { this.comments.update((l) => [...l, c]); this.draft.set(''); this.commentBusy.set(false); },
      error: (e) => { this.error.set(readError(e)); this.commentBusy.set(false); },
    });
  }

  // ----- T28 -----
  protected toggleAvailability(): void {
    const next = !this.available();
    this.availabilityBusy.set(true);
    this.error.set('');
    this.workApi.setAvailability(next).subscribe({
      next: async (r) => {
        this.available.set(r.available);
        this.info.set(r.available ? 'You are now available for new assignments.' : 'You are now unavailable.');
        await supabase.auth.refreshSession(); // garde la session locale à jour
        this.availabilityBusy.set(false);
      },
      error: (e) => { this.error.set(readError(e)); this.availabilityBusy.set(false); },
    });
  }
}