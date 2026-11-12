import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthApi, Role } from '../auth/auth-api';
import { readError } from '../report-api';
import { AdminUser, UsersApi } from '../users-api';

@Component({
  selector: 'app-users-page',
  imports: [FormsModule],
  template: `
    <main class="wrap">
      <header class="top">
        <div>
          <h1>User management</h1>
          <p class="muted">{{ filtered().length }} user(s)</p>
        </div>
        <div class="tools">
          <input
            class="search"
            placeholder="Search by name or email"
            [ngModel]="query()"
            (ngModelChange)="query.set($event)"
          />
          <select
            class="search"
            [ngModel]="roleFilter()"
            (ngModelChange)="roleFilter.set($event)"
          >
            <option value="all">All roles</option>
            @for (r of roles; track r) { <option [value]="r">{{ roleLabel(r) }}</option> }
          </select>
          <button type="button" class="primary" (click)="openCreate()">+ New user</button>
        </div>
      </header>

      @if (error()) { <p class="err">{{ error() }}</p> }
      @if (info()) { <p class="ok">{{ info() }}</p> }

      @if (formOpen()) {
        <section class="card">
          <h2>{{ editingId() ? 'Edit user' : 'New user' }}</h2>
          <form (ngSubmit)="save()" class="form">
            <label>Full name
              <input name="fullName" [(ngModel)]="form.fullName" required />
            </label>
            <label>Email
              <input name="email" type="email" autocomplete="off" [(ngModel)]="form.email" [disabled]="!!editingId()" required />
            </label>
            @if (!editingId()) {
              <label>Password
                <input name="password" type="password" autocomplete="new-password" [(ngModel)]="form.password" />
              </label>
            }
            <label>Phone
              <input name="phone" [(ngModel)]="form.phone" />
            </label>
            <label>Role
              <select name="role" [(ngModel)]="form.role">
                @for (r of roles; track r) { <option [value]="r">{{ roleLabel(r) }}</option> }
              </select>
            </label>
            <div class="actions">
              <button type="submit" class="primary" [disabled]="saving()">Save</button>
              <button type="button" (click)="closeForm()">Cancel</button>
            </div>
          </form>
        </section>
      }

      <section class="card table-card">
        @if (loading()) {
          <p class="muted">Loading…</p>
        } @else {
          <table>
            <thead>
              <tr><th>Name</th><th>Email</th><th>Phone</th><th>Role</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              @for (u of filtered(); track u.id) {
                <tr>
                  <td>{{ u.fullName || '—' }}</td>
                  <td>{{ u.email }}</td>
                  <td>{{ u.phone || '—' }}</td>
                  <td><span class="badge">{{ roleLabel(u.role) }}</span></td>
                  <td>
                    <span class="badge" [class.off]="u.suspended">
                      {{ u.suspended ? 'suspended' : 'active' }}
                    </span>
                  </td>
                  <td class="row-actions">
                    <button type="button" (click)="openEdit(u)">Edit</button>
                    <button type="button" [disabled]="isMe(u)" (click)="toggleSuspend(u)">
                      {{ u.suspended ? 'Activate' : 'Suspend' }}
                    </button>
                    <button type="button" class="danger" [disabled]="isMe(u)" (click)="remove(u)">
                      Delete
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="6" class="muted">No users found.</td></tr>
              }
            </tbody>
          </table>
        }
      </section>
    </main>
  `,
  styles: [`
    .wrap { max-width: 1100px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; color: #1c1c1a; }
    .top { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; }
    h1, h2 { margin: 0; }
    .muted { color: #6b6b66; margin: .25rem 0 0; }
    .tools { display: flex; gap: .75rem; flex-wrap: wrap; }
    .search { padding: .6rem .9rem; border: 1px solid #d6d0c2; border-radius: 999px; min-width: 200px; font: inherit; background: #fff; }
    .card { background: #fffdf8; border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); }
    .table-card { overflow-x: auto; }
    .form { display: grid; gap: .9rem; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); margin-top: 1rem; }
    label { display: grid; gap: .35rem; font-weight: 600; }
    input, select { padding: .65rem .85rem; border: 1px solid #d6d0c2; border-radius: 12px; font: inherit; }
    .actions { display: flex; gap: .75rem; align-items: end; }
    button { padding: .55rem 1rem; border: 1px solid #d6d0c2; border-radius: 999px; background: #fff; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .45; cursor: default; }
    .primary { background: #d92f2f; color: #fff; border-color: #d92f2f; }
    .danger { color: #b3261e; border-color: #e3b5b1; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: .75rem .6rem; border-bottom: 1px solid #e6e1d6; }
    th { font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; color: #6b6b66; }
    .row-actions { display: flex; gap: .4rem; flex-wrap: wrap; }
    .badge { display: inline-block; padding: .2rem .7rem; border-radius: 999px; background: #ece6d8; font-size: .8rem; font-weight: 700; }
    .badge.off { background: #f6d6d3; color: #b3261e; }
    .err { color: #b3261e; margin: 0; }
    .ok { color: #1b7f3b; margin: 0; }
  `],
})
export class UsersPage implements OnInit {
  private readonly api = inject(UsersApi);
  protected readonly auth = inject(AuthApi);

  protected readonly roles: Role[] = ['citizen', 'agent', 'sector_manager', 'admin'];
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly info = signal('');
  protected readonly formOpen = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly query = signal('');
  protected readonly roleFilter = signal<'all' | Role>('all');

  protected form = { fullName: '', email: '', phone: '', password: '', role: 'citizen' as Role };

  protected readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const role = this.roleFilter();
    return this.users().filter(
      (u) =>
        (role === 'all' || u.role === role) &&
        (!q || u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  protected roleLabel(role: Role): string {
    return role === 'sector_manager' ? 'Sector Manager' : role;
  }

  protected isMe(u: AdminUser): boolean {
    return u.id === this.auth.user()?.id;
  }

  protected openCreate(): void {
    this.editingId.set(null);
    this.form = { fullName: '', email: '', phone: '', password: '', role: 'citizen' };
    this.error.set('');
    this.formOpen.set(true);
  }

  protected openEdit(u: AdminUser): void {
    this.editingId.set(u.id);
    this.form = { fullName: u.fullName, email: u.email, phone: u.phone ?? '', password: '', role: u.role };
    this.error.set('');
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
  }

  protected save(): void {
    this.error.set('');
    this.info.set('');
    const f = this.form;
    const id = this.editingId();

    if (!f.fullName.trim()) {
      this.error.set('Full name is required.');
      return;
    }
    if (!id && (!f.email.trim() || f.password.length < 6)) {
      this.error.set('Email and a password of at least 6 characters are required.');
      return;
    }

    this.saving.set(true);
    const request = id
      ? this.api.update(id, { fullName: f.fullName.trim(), phone: f.phone.trim(), role: f.role })
      : this.api.create({
          email: f.email.trim(),
          password: f.password,
          fullName: f.fullName.trim(),
          phone: f.phone.trim() || null,
          role: f.role,
        });

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.info.set(id ? 'User updated.' : 'User created.');
        this.closeForm();
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.error.set(readError(e));
      },
    });
  }

  protected toggleSuspend(u: AdminUser): void {
    this.error.set('');
    this.info.set('');
    this.api.update(u.id, { suspended: !u.suspended }).subscribe({
      next: () => {
        this.info.set(u.suspended ? 'User activated.' : 'User suspended.');
        this.load();
      },
      error: (e) => this.error.set(readError(e)),
    });
  }

  protected remove(u: AdminUser): void {
    if (!confirm(`Delete ${u.email}? This cannot be undone.`)) return;
    this.error.set('');
    this.info.set('');
    this.api.remove(u.id).subscribe({
      next: () => {
        this.info.set('User deleted.');
        this.load();
      },
      error: (e) => this.error.set(readError(e)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (list) => {
        this.users.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(readError(e));
        this.loading.set(false);
      },
    });
  }
}