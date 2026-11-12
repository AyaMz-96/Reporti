import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ReportListApi } from '../report-list-api';
import { AgentApi } from '../agent-api';
import { ReportResponse } from '../models';
import { readError } from '../report-api';

@Component({
  selector: 'app-report-assignment-page',
  imports: [DatePipe],
  template: `
    <main class="wrap">
      <header class="top">
        <div>
          <h1>Report Assignments</h1>
          <p class="muted">Assign available agents to pending reports</p>
        </div>
      </header>

      @if (error()) { <p class="err">{{ error() }}</p> }
      @if (info()) { <p class="ok">{{ info() }}</p> }

      <section class="card table-card">
        @if (loading()) {
          <p class="muted">Loading reports…</p>
        } @else {
          <table>
            <thead>
              <tr><th>ID</th><th>Title</th><th>Category</th><th>Status</th><th>Date</th><th></th></tr>
            </thead>
            <tbody>
              @for (report of reports(); track report.id) {
                <tr>
                  <td>{{ report.id }}</td>
                  <td>{{ report.title }}</td>
                  <td>{{ categoryName(report.categoryId) }}</td>
                  <td>
                    <span class="badge">{{ report.status.replace('_', ' ') }}</span>
                  </td>
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
    </main>

    @if (assigningReport()) {
      <div class="modal-backdrop">
        <section class="modal">
          <h2>Proposed Agents</h2>
          <p class="muted">Select an agent for: <strong>{{ assigningReport()?.title }}</strong></p>

          @if (agentsLoading()) {
            <p class="muted" style="margin-top: 1rem;">Loading available agents…</p>
          } @else if (availableAgents().length === 0) {
            <p class="muted" style="margin-top: 1rem;">No available agents found in your sector/category.</p>
          } @else {
            <ul class="agent-list">
              @for (agent of availableAgents(); track agent.id) {
                <li>
                  <div>
                    <strong>{{ agent.user_metadata?.full_name || agent.email }}</strong>
                    <br><small>{{ agent.email }}</small>
                  </div>
                  <button type="button" class="primary" (click)="assign(agent.id)">Assign</button>
                </li>
              }
            </ul>
          }
          <div class="actions" style="margin-top: 1rem;">
            <button type="button" (click)="closeAssign()">Cancel</button>
          </div>
        </section>
      </div>
    }
  `,
  styles: [`
    .wrap { max-width: 1100px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; color: #1c1c1a; }
    .top { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
    h1, h2 { margin: 0; }
    .muted { color: #6b6b66; margin: .25rem 0 0; }
    .card { background: #fffdf8; border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); }
    .table-card { overflow-x: auto; }
    button { padding: .55rem 1rem; border: 1px solid #d6d0c2; border-radius: 999px; background: #fff; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .45; cursor: default; }
    .primary { background: #d92f2f; color: #fff; border-color: #d92f2f; }
    .primary:hover:not(:disabled) { filter: brightness(1.1); }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: .75rem .6rem; border-bottom: 1px solid #e6e1d6; }
    th { font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; color: #6b6b66; }
    .badge { display: inline-block; padding: .2rem .7rem; border-radius: 999px; background: #ece6d8; font-size: .8rem; font-weight: 700; }
    .err { color: #b3261e; margin: 0; }
    .ok { color: #1b7f3b; margin: 0; }
    
    .modal-backdrop { position: fixed; inset: 0; background: rgba(28, 25, 23, 0.4); display: grid; place-items: center; z-index: 2000; backdrop-filter: blur(4px); }
    .modal { background: #fffdf8; border-radius: 20px; padding: 2rem; width: min(500px, 90vw); box-shadow: 0 24px 70px rgba(40, 35, 27, 0.25); }
    .agent-list { list-style: none; padding: 0; margin: 1rem 0; display: grid; gap: .75rem; max-height: 300px; overflow-y: auto; }
    .agent-list li { display: flex; justify-content: space-between; align-items: center; padding: 1rem; border: 1px solid #e6e1d6; border-radius: 12px; }
    .actions { display: flex; justify-content: flex-end; }
  `]
})
export class ReportAssignmentPage implements OnInit {
  private readonly reportApi = inject(ReportListApi);
  private readonly agentApi = inject(AgentApi);

  protected readonly reports = signal<ReportResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly assigningReport = signal<ReportResponse | null>(null);
  protected readonly availableAgents = signal<any[]>([]);
  protected readonly agentsLoading = signal(false);
  protected readonly error = signal('');
  protected readonly info = signal('');
  protected readonly categories = signal<any[]>([]);

  ngOnInit(): void {
    this.loadData();
  }

  protected categoryName(id: number): string {
    return this.categories().find(c => c.id === id)?.name ?? `#${id}`;
  }

  protected openAssign(report: ReportResponse): void {
    this.assigningReport.set(report);
    this.agentsLoading.set(true);
    this.agentApi.getAvailableAgents().subscribe({
      next: (agents) => {
        this.availableAgents.set(agents);
        this.agentsLoading.set(false);
      },
      error: (e) => {
        this.error.set(readError(e));
        this.agentsLoading.set(false);
      }
    });
  }

  protected closeAssign(): void {
    this.assigningReport.set(null);
    this.availableAgents.set([]);
  }

  protected assign(agentId: string): void {
    const report = this.assigningReport();
    if (!report) return;

    this.agentApi.assignReport(report.id, agentId).subscribe({
      next: () => {
        this.info.set(`Report #${report.id} assigned successfully and status changed to IN PROGRESS.`);
        this.closeAssign();
        this.loadData();
      },
      error: (e) => {
        this.error.set(readError(e));
      }
    });
  }

  private loadData(): void {
    this.loading.set(true);
    this.reportApi.getReports().subscribe({
      next: (list) => {
        this.reports.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(readError(e));
        this.loading.set(false);
      }
    });
    this.reportApi.getCategories().subscribe({
      next: (cats) => this.categories.set(cats)
    });
  }
}
