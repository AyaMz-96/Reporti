import { Component, OnInit, inject, signal } from '@angular/core';

import { AuthApi } from '../auth/auth-api';
import { UsersApi } from '../users-api';

@Component({
  selector: 'app-dashboard-page',
  template: `
    <main class="wrap">
      @if (auth.user(); as user) {
        <section class="card">
          <h1>Welcome, {{ user.fullName }}</h1>
          <p class="muted">Your space · {{ user.role }}</p>
        </section>

        <section class="stats">
          <div class="card stat"><strong>{{ total() }}</strong><span>Users</span></div>
          <div class="card stat"><strong>{{ agents() }}</strong><span>Agents</span></div>
          <div class="card stat"><strong>{{ suspended() }}</strong><span>Suspended</span></div>
        </section>

        <p class="muted" style="margin-top: 0.5rem;">
          Use the <strong>Admin ▾</strong> menu in the top navigation to manage users, agents, categories and assignments.
        </p>
      }
    </main>
  `,
  styles: [`
    .wrap { max-width: 900px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; color: var(--text-main); }
    .card { background: var(--surface-solid); border: 1px solid var(--border); border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); color: var(--text-main); }
    h1 { margin: 0 0 .5rem; color: var(--text-main); }
    .muted { color: var(--muted); margin: 0; }
    .stats { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); }
    .stat { display: grid; gap: .25rem; text-align: center; }
    .stat strong { font-size: 2rem; color: var(--accent-red); }
  `],
})
export class DashboardPage implements OnInit {
  protected readonly auth = inject(AuthApi);
  private readonly users = inject(UsersApi);
  protected readonly total = signal(0);
  protected readonly agents = signal(0);
  protected readonly suspended = signal(0);

  ngOnInit(): void {
    this.users.list().subscribe((list) => {
      this.total.set(list.length);
      this.agents.set(list.filter((u) => u.role === 'agent').length);
      this.suspended.set(list.filter((u) => u.suspended).length);
    });
  }
}