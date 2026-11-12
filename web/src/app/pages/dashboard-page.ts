import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthApi } from '../auth/auth-api';
import { UsersApi } from '../users-api';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink],
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
        <section class="links">
          <a class="card link" routerLink="/admin/users">Manage users</a>
          <a class="card link" routerLink="/admin/agents">Manage agents</a>
          <a class="card link" routerLink="/admin/categories">Manage categories</a>
          <a class="card link" routerLink="/agent/assignments">Assign Reports</a>
          <a class="card link" routerLink="/reports">All reports</a>
          <a class="card link" routerLink="/map">Reports map</a>
        </section>

        <a class="card link" routerLink="/account">My profile</a>
      }
    </main>
  `,
  styles: [`
    .wrap { max-width: 900px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; color: #1c1c1a; }
    .card { background: #fffdf8; border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); }
    h1, h2 { margin: 0 0 .5rem; }
    .muted { color: #6b6b66; margin: 0; }
    .stats, .links { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); }
    .stat { display: grid; gap: .25rem; text-align: center; }
    .stat strong { font-size: 2rem; color: #d92f2f; }
    .link { text-decoration: none; color: inherit; font-weight: 700; text-align: center; }
    .link:hover { background: #fff; }
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
