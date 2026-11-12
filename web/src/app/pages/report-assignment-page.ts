import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ReportListApi } from '../report-list-api';
import { AgentApi } from '../agent-api';
import { ReportResponse } from '../models';
import { readError } from '../report-api';
import { AuthApi } from '../auth/auth-api';

@Component({
  selector: 'app-report-assignment-page',
  imports: [DatePipe],
  template: `
    <main class="wrap">
      <header class="top">
        <div>
          @if (auth.isAdmin()) {
            <h1>Report Assignments</h1>
            <p class="muted">Assign available agents to pending reports</p>
          } @else {
            <h1>Available for me</h1>
            <p class="muted">Tasks you are fit to take up</p>
          }
        </div>
      </header>

      @if (error()) { <p class="err">{{ error() }}</p> }
      @if (info()) { <p class="ok">{{ info() }}</p> }

      @if (auth.isAdmin()) {
        <!-- ============ ADMIN: dispatch ============ -->
        <section class="card table-card">
          @if (loading()) {
            <p class="muted">Loading reports…</p>
          } @else {
            <table>
              <thead><tr><th>ID</th><th>Title</th><th>Category</th><th>Status</th><th>Date</th><th></th></tr></thead>
              <tbody>
                @for (report of reports(); track report.id) {
                  <tr>
                    <td>{{ report.id }}</td>
                    <td>{{ report.title }}</td>
                    <td>{{ categoryName(report.categoryId) }}</td>
                    <td><span class="badge">{{ report.status.replace('_', ' ') }}</span></td>
                    <td>{{ report.createdAt | date: 'short' }}</td>
                    <td>
                      <button type="button" class="primary" (click)="openAssign(report)" [disabled]="report.status !== 'RECEIVED'">
                        Assign Agent
                      </button>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="6" class="muted">No reports found.</td></tr>
                }
              </tbody>
            </table>
          }
        </section>
      } @else {
        <!-- ============ AGENT: claim flow ============ -->
        @if (profileEmpty()) {
          <section class="card">
            <p class="muted">
              Your profile has no categories or sector configured yet. Set them up on your
              <a routerLink="/account">Account page</a> to see reports you're fit for.
            </p>
          </section>
        }

        <section class="card">
          <h2>Available for you</h2>
          <p class="muted">Reports you can take up right now</p>
          @if (availableLoading()) {
            <p class="muted" style="margin-top: 1rem;">Loading…</p>
          } @else if (myAvailable().length === 0) {
            <p class="muted" style="margin-top: 1rem;">No new reports match your profile right now.</p>
          } @else {
            <ul class="task-list">
              @for (report of myAvailable(); track report.id) {
                <li>
                  <div>
                    <strong>#{{ report.id }} · {{ report.title }}</strong>
                    <div class="small">{{ categoryName(report.categoryId) }} · {{ sectorName(report.sectorId) }} · {{ report.createdAt | date: 'short' }}</div>
                  </div>
                  <button type="button" class="primary" (click)="openTake(report)">Take this task</button>
                </li>
              }
            </ul>
          }
        </section>

        <section class="card">
          <h2>Your active tasks</h2>
          <p class="muted">Reports you have already taken up</p>
          @if (activeLoading()) {
            <p class="muted" style="margin-top: 1rem;">Loading…</p>
          } @else if (myActive().length === 0) {
            <p class="muted" style="margin-top: 1rem;">You haven't taken any tasks yet.</p>
          } @else {
            <ul class="task-list">
              @for (report of myActive(); track report.id) {
                <li>
                  <div>
                    <strong>#{{ report.id }} · {{ report.title }}</strong>
                    <div class="small">{{ categoryName(report.categoryId) }} · {{ sectorName(report.sectorId) }} · <span class="badge">{{ report.status.replace('_', ' ') }}</span></div>
                  </div>
                </li>
              }
            </ul>
          }
        </section>
      }
    </main>

    <!-- ============ ADMIN modal: pick agent ============ -->
    @if (assigningReport()) {
      <div class="modal-backdrop">
        <section class="modal">
          <h2>Proposed Agents</h2>
          <p class="muted">Report: <strong>{{ assigningReport()?.title }}</strong> · Category: <strong>{{ categoryName(assigningReport()!.categoryId) }}</strong> · Sector: <strong>{{ sectorName(assigningReport()!.sectorId) }}</strong></p>
          @if (agentsLoading()) {
            <p class="muted" style="margin-top: 1rem;">Loading available agents…</p>
          } @else if (availableAgents().length === 0) {
            <p class="muted" style="margin-top: 1rem;">No available agents match this report's category and sector.</p>
          } @else {
            <ul class="task-list">
              @for (agent of availableAgents(); track agent.id) {
                <li>
                  <div>
                    <strong>{{ agent.user_metadata?.full_name || agent.email }}</strong>
                    <div class="small">{{ agent.email }}</div>
                  </div>
                  <button type="button" class="primary" (click)="assign(agent.id)">Assign</button>
                </li>
              }
            </ul>
          }
          <div class="actions">
            <button type="button" class="action-btn" (click)="closeAssign()">Cancel</button>
          </div>
        </section>
      </div>
    }

    <!-- ============ AGENT modal: confirm take ============ -->
    @if (confirmingTake()) {
      <div class="modal-backdrop">
        <section class="modal">
          <h2>Take this task?</h2>
          <p class="muted" style="margin-top: .5rem;">
            You are about to take:<br>
            <strong>#{{ confirmingTake()!.id }} · {{ confirmingTake()!.title }}</strong><br>
            <span class="small">{{ categoryName(confirmingTake()!.categoryId) }} · {{ sectorName(confirmingTake()!.sectorId) }}</span>
          </p>
          <p class="muted" style="margin-top: .75rem;">The report will be assigned to you and its status will become IN PROGRESS.</p>
          <div class="actions" style="margin-top: 1rem; gap: .5rem;">
            <button type="button" class="action-btn" (click)="closeTake()">Cancel</button>
            <button type="button" class="primary" [disabled]="claiming()" (click)="confirmTake()">
              {{ claiming() ? 'Taking…' : 'Yes, take it' }}
            </button>
          </div>
        </section>
      </div>
    }
  `,
  styles: [`
    :host { display: block; color: var(--text-main); }
    .wrap { max-width: 1100px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; }
    .top { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
    h1, h2 { margin: 0; color: var(--text-main); }
    .muted { color: var(--muted); margin: .25rem 0 0; }
    .small { font-size: .82rem; color: var(--muted); }
    .card { background: var(--surface-solid); border: 1px solid var(--border); border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); color: var(--text-main); }
    .card + .card { margin-top: 0; }
    .table-card { overflow-x: auto; }
    button { padding: .55rem 1rem; border: 1px solid var(--border); border-radius: 999px; background: var(--surface-solid); color: var(--text-main); font-weight: 600; cursor: pointer; font: inherit; }
    button:disabled { opacity: .45; cursor: default; }
    .primary { background: var(--accent-red); color: #fff; border-color: var(--accent-red); }
    .danger { color: var(--accent-red); border-color: var(--accent-red); }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: .75rem .6rem; border-bottom: 1px solid var(--border); color: var(--text-main); }
    th { font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); }
    .badge { display: inline-block; padding: .2rem .7rem; border-radius: 999px; background: var(--surface); border: 1px solid var(--border); font-size: .8rem; font-weight: 700; color: var(--text-main); }
    .err { color: var(--accent-red); margin: 0; }
    .ok { color: #22c55e; margin: 0; }
    a { color: var(--accent-red); font-weight: 600; }
    .task-list { list-style: none; padding: 0; margin: 1rem 0 0; display: grid; gap: .75rem; }
    .task-list li { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 1rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); color: var(--text-main); }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(28, 25, 23, 0.4); display: grid; place-items: center; z-index: 2000; backdrop-filter: blur(4px); }
    .modal { background: var(--surface-solid); color: var(--text-main); border: 1px solid var(--border); border-radius: 20px; padding: 2rem; width: min(520px, 90vw); box-shadow: 0 24px 70px rgba(40, 35, 27, 0.25); }
    .actions { display: flex; justify-content: flex-end; gap: .5rem; margin-top: 1rem; }

    :host-context(.dark-theme) .action-btn {
      background: #50524c !important;
      color: #F7F4E8 !important;
      border-color: rgba(255,255,255,0.18) !important;
    }
    :host-context(.dark-theme) .primary { background: #FF767B !important; color: #1a1a1a !important; }
  `]
})
export class ReportAssignmentPage implements OnInit {
  private readonly reportApi = inject(ReportListApi);
  private readonly agentApi = inject(AgentApi);
  protected readonly auth = inject(AuthApi);

  // Admin
  protected readonly reports = signal<ReportResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly assigningReport = signal<ReportResponse | null>(null);
  protected readonly availableAgents = signal<any[]>([]);
  protected readonly agentsLoading = signal(false);

  // Agent
  protected readonly myAvailable = signal<ReportResponse[]>([]);
  protected readonly myActive = signal<ReportResponse[]>([]);
  protected readonly availableLoading = signal(false);
  protected readonly activeLoading = signal(false);
  protected readonly confirmingTake = signal<ReportResponse | null>(null);
  protected readonly claiming = signal(false);

  // Shared
  protected readonly error = signal('');
  protected readonly info = signal('');
  protected readonly categories = signal<any[]>([]);
  protected readonly sectors = signal<any[]>([]);

  ngOnInit(): void {
    this.reportApi.getCategories().subscribe((c) => this.categories.set(c));
    this.reportApi.getSectors().subscribe((s) => this.sectors.set(s));
    if (this.auth.isAdmin()) {
      this.loadAdmin();
    } else {
      this.loadAgent();
    }
  }

  protected categoryName(id: number): string { return this.categories().find((c) => c.id === id)?.name ?? `#${id}`; }
  protected sectorName(id: number): string { return this.sectors().find((s) => s.id === id)?.name ?? `#${id}`; }
  protected profileEmpty(): boolean {
    const u = this.auth.user();
    if (!u) return true;
    return u.categoryIds.length === 0 && u.sectorId == null;
  }

  // ----- ADMIN -----
  private loadAdmin(): void {
    this.loading.set(true);
    this.reportApi.getReports().subscribe({
      next: (list) => { this.reports.set(list); this.loading.set(false); },
      error: (e) => { this.error.set(readError(e)); this.loading.set(false); }
    });
  }
  protected openAssign(report: ReportResponse): void {
    this.assigningReport.set(report);
    this.agentsLoading.set(true);
    this.agentApi.getAvailableAgents(report.categoryId, report.sectorId).subscribe({
      next: (agents) => { this.availableAgents.set(agents); this.agentsLoading.set(false); },
      error: (e) => { this.error.set(readError(e)); this.agentsLoading.set(false); }
    });
  }
  protected closeAssign(): void { this.assigningReport.set(null); this.availableAgents.set([]); }
  protected assign(agentId: string): void {
    const report = this.assigningReport();
    if (!report) return;
    this.agentApi.assignReport(report.id, agentId).subscribe({
      next: () => { this.info.set(`Report #${report.id} assigned. Status is now IN PROGRESS.`); this.closeAssign(); this.loadAdmin(); },
      error: (e) => this.error.set(readError(e)),
    });
  }

  // ----- AGENT -----
  private loadAgent(): void {
    this.availableLoading.set(true);
    this.agentApi.getMyAvailable().subscribe({
      next: (list) => { this.myAvailable.set(list); this.availableLoading.set(false); },
      error: (e) => { this.error.set(readError(e)); this.availableLoading.set(false); }
    });
    this.activeLoading.set(true);
    this.agentApi.getMyActive().subscribe({
      next: (list) => { this.myActive.set(list); this.activeLoading.set(false); },
      error: (e) => { this.error.set(readError(e)); this.activeLoading.set(false); }
    });
  }
  protected openTake(report: ReportResponse): void { this.confirmingTake.set(report); this.error.set(''); }
  protected closeTake(): void { this.confirmingTake.set(null); }
  protected confirmTake(): void {
    const report = this.confirmingTake();
    if (!report) return;
    this.claiming.set(true);
    this.error.set('');
    this.agentApi.claimReport(report.id).subscribe({
      next: () => {
        this.claiming.set(false);
        this.info.set(`You took report #${report.id}. It's now IN PROGRESS.`);
        this.closeTake();
        this.loadAgent();
      },
      error: (e) => { this.claiming.set(false); this.error.set(readError(e)); },
    });
  }
}
