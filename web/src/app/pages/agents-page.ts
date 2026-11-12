import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminUser, UsersApi } from '../users-api';
import { readError, ReportApi } from '../report-api';
import { AuthApi } from '../auth/auth-api';
import { Category, Sector } from '../models';

@Component({
  selector: 'app-agents-page',
  imports: [FormsModule],
  template: `
    <main class="wrap">
      <header class="top">
        <div>
          <h1>Agent management</h1>
          <p class="muted">{{ filtered().length }} agent(s)</p>
        </div>
        <div class="tools">
          <input class="search" placeholder="Search by name or email" [ngModel]="query()" (ngModelChange)="query.set($event)" />
          <button type="button" class="primary" (click)="openCreate()">+ New agent</button>
        </div>
      </header>

      @if (error()) { <p class="err">{{ error() }}</p> }
      @if (info()) { <p class="ok">{{ info() }}</p> }

      @if (formOpen()) {
        <section class="card">
          <h2>{{ editingId() ? 'Edit agent' : 'New agent' }}</h2>
          <form (ngSubmit)="save()" class="form">
            <label>Full name <input name="fullName" [(ngModel)]="form.fullName" required /></label>
            <label>Email <input name="email" type="email" [(ngModel)]="form.email" [disabled]="!!editingId()" required /></label>
            @if (!editingId()) {
              <label>Password <input name="password" type="password" [(ngModel)]="form.password" /></label>
            }
            <label>Phone <input name="phone" [(ngModel)]="form.phone" /></label>

            <label>Sector
              <select name="sectorId" [(ngModel)]="form.sectorId">
                <option [ngValue]="null">— Any —</option>
                @for (s of sectors(); track s.id) {
                  <option [ngValue]="s.id">{{ s.name }}{{ s.zone ? ' (' + s.zone + ')' : '' }}</option>
                }
              </select>
            </label>

            <div class="checkbox-group">
              <span class="group-label">Categories (select all that apply)</span>
              @for (c of categories(); track c.id) {
                <label class="checkbox-row">
                  <input type="checkbox" [checked]="form.categoryIds.includes(c.id)"
                         (change)="toggleCategory(c.id)" />
                  {{ c.name }}
                </label>
              } @empty {
                <span class="muted">No categories created yet.</span>
              }
            </div>

            <div class="actions">
              <button type="submit" class="primary" [disabled]="saving()">Save</button>
              <button type="button" class="action-btn" (click)="closeForm()">Cancel</button>
            </div>
          </form>
        </section>
      }

      <section class="card table-card">
        @if (loading()) {
          <p class="muted">Loading…</p>
        } @else {
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Categories</th><th>Sector</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (u of filtered(); track u.id) {
                <tr>
                  <td>{{ u.fullName || '—' }}</td>
                  <td>{{ u.email }}</td>
                  <td>{{ u.phone || '—' }}</td>
                  <td>{{ categoryNames(u.categoryIds) }}</td>
                  <td>{{ sectorName(u.sectorId) }}</td>
                  <td><span class="badge" [class.off]="u.suspended">{{ u.suspended ? 'suspended' : 'active' }}</span></td>
                  <td class="row-actions">
                    <button type="button" class="action-btn" (click)="openEdit(u)">Edit</button>
                    <button type="button" class="action-btn" [disabled]="isMe(u)" (click)="toggleSuspend(u)">{{ u.suspended ? 'Activate' : 'Suspend' }}</button>
                    <button type="button" class="action-btn danger" [disabled]="isMe(u)" (click)="remove(u)">Delete</button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="muted">No agents found.</td></tr>
              }
            </tbody>
          </table>
        }
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; color: var(--text-main); }
    .wrap { max-width: 1100px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; }
    .top { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; }
    h1, h2 { margin: 0; color: var(--text-main); }
    .muted { color: var(--muted); margin: .25rem 0 0; }
    .tools { display: flex; gap: .75rem; flex-wrap: wrap; }
    .search { padding: .6rem .9rem; border: 1px solid var(--border); border-radius: 999px; min-width: 200px; font: inherit; background: var(--surface-solid); color: var(--text-main); }
    .card { background: var(--surface-solid); border: 1px solid var(--border); border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); color: var(--text-main); }
    .table-card { overflow-x: auto; }
    .form { display: grid; gap: .9rem; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); margin-top: 1rem; }
    label { display: grid; gap: .35rem; font-weight: 600; color: var(--text-main); }
    input, select { padding: .65rem .85rem; border: 1px solid var(--border); border-radius: 12px; font: inherit; background: var(--surface-solid); color: var(--text-main); }
    .checkbox-group { grid-column: 1 / -1; display: flex; flex-wrap: wrap; gap: .5rem 1.25rem; padding: .75rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .group-label { width: 100%; font-weight: 600; font-size: .85rem; color: var(--muted); }
    .checkbox-row { display: flex; align-items: center; gap: .35rem; font-weight: 500; }
    .checkbox-row input { width: auto; }
    .actions { display: flex; gap: .75rem; align-items: end; grid-column: 1 / -1; }
    button { padding: .55rem 1rem; border: 1px solid var(--border); border-radius: 999px; background: var(--surface-solid); color: var(--text-main); font-weight: 600; cursor: pointer; font: inherit; }
    button:disabled { opacity: .45; cursor: default; }
    .primary { background: var(--accent-red); color: #fff; border-color: var(--accent-red); }
    .danger { color: var(--accent-red); border-color: var(--accent-red); }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: .75rem .6rem; border-bottom: 1px solid var(--border); color: var(--text-main); }
    th { font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); }
    .row-actions { display: flex; gap: .4rem; flex-wrap: wrap; }
    .badge { display: inline-block; padding: .2rem .7rem; border-radius: 999px; background: var(--surface); border: 1px solid var(--border); font-size: .8rem; font-weight: 700; color: var(--text-main); }
    .badge.off { background: rgba(214, 48, 49, 0.15); color: var(--accent-red); border-color: var(--accent-red); }
    .err { color: var(--accent-red); margin: 0; }
    .ok { color: #22c55e; margin: 0; }

    :host-context(.dark-theme) .action-btn {
      background: #50524c !important;
      color: #F7F4E8 !important;
      border-color: rgba(255,255,255,0.18) !important;
    }
    :host-context(.dark-theme) .action-btn.danger { color: #FF767B !important; border-color: #FF767B !important; }
    :host-context(.dark-theme) .primary { background: #FF767B !important; color: #1a1a1a !important; }
  `]
})
export class AgentsPage implements OnInit {
  private readonly api = inject(UsersApi);
  private readonly reportApi = inject(ReportApi);
  private readonly auth = inject(AuthApi);

  protected readonly users = signal<AdminUser[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly info = signal('');
  protected readonly formOpen = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly query = signal('');
  protected readonly categories = signal<Category[]>([]);
  protected readonly sectors = signal<Sector[]>([]);

  protected form = { fullName: '', email: '', phone: '', password: '', categoryIds: [] as number[], sectorId: null as number | null };

  protected readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.users().filter(
      (u) => u.role === 'agent' && (!q || u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
    );
  });

  ngOnInit(): void {
    this.load();
    this.reportApi.getCategories().subscribe((c) => this.categories.set(c));
    this.reportApi.getSectors().subscribe((s) => this.sectors.set(s));
  }

  protected categoryNames(ids: number[]): string {
    if (!ids || ids.length === 0) return '—';
    return ids.map((id) => this.categories().find((c) => c.id === id)?.name ?? `#${id}`).join(', ');
  }

  protected sectorName(id: number | null): string {
    if (id == null) return 'Any';
    return this.sectors().find((s) => s.id === id)?.name ?? `#${id}`;
  }

  protected isMe(u: AdminUser): boolean { return u.id === this.auth.user()?.id; }

  protected toggleCategory(id: number): void {
    const set = new Set(this.form.categoryIds);
    if (set.has(id)) set.delete(id); else set.add(id);
    this.form = { ...this.form, categoryIds: Array.from(set) };
  }

  protected openCreate(): void {
    this.editingId.set(null);
    this.form = { fullName: '', email: '', phone: '', password: '', categoryIds: [], sectorId: null };
    this.error.set('');
    this.formOpen.set(true);
  }

  protected openEdit(u: AdminUser): void {
    this.editingId.set(u.id);
    this.form = { fullName: u.fullName, email: u.email, phone: u.phone ?? '', password: '', categoryIds: [...u.categoryIds], sectorId: u.sectorId };
    this.error.set('');
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
  }

  protected save(): void {
    this.error.set(''); this.info.set('');
    const f = this.form; const id = this.editingId();
    if (!f.fullName.trim()) { this.error.set('Full name is required.'); return; }
    if (!id && (!f.email.trim() || f.password.length < 6)) { this.error.set('Email and a password of at least 6 characters are required.'); return; }

    this.saving.set(true);
    const request = id
      ? this.api.update(id, { fullName: f.fullName.trim(), phone: f.phone.trim(), categoryIds: f.categoryIds, sectorId: f.sectorId })
      : this.api.create({
          email: f.email.trim(),
          password: f.password,
          fullName: f.fullName.trim(),
          phone: f.phone.trim() || null,
          role: 'agent',
          categoryIds: f.categoryIds,
          sectorId: f.sectorId,
        });

    request.subscribe({
      next: () => { this.saving.set(false); this.info.set(id ? 'Agent updated.' : 'Agent created.'); this.closeForm(); this.load(); },
      error: (e) => { this.saving.set(false); this.error.set(readError(e)); },
    });
  }

  protected toggleSuspend(u: AdminUser): void {
    this.error.set(''); this.info.set('');
    this.api.update(u.id, { suspended: !u.suspended }).subscribe({
      next: () => { this.info.set(u.suspended ? 'Agent activated.' : 'Agent suspended.'); this.load(); },
      error: (e) => this.error.set(readError(e)),
    });
  }

  protected remove(u: AdminUser): void {
    if (!confirm(`Delete agent ${u.email}? This cannot be undone.`)) return;
    this.error.set(''); this.info.set('');
    this.api.remove(u.id).subscribe({
      next: () => { this.info.set('Agent deleted.'); this.load(); },
      error: (e) => this.error.set(readError(e)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (list) => { this.users.set(list); this.loading.set(false); },
      error: (e) => { this.error.set(readError(e)); this.loading.set(false); },
    });
  }
}
