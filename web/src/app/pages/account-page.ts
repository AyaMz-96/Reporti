import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthApi, errorMessage, supabase } from '../auth/auth-api';
import { ReportApi } from '../report-api';
import { Category, Sector } from '../models';

@Component({
  selector: 'app-account-page',
  imports: [FormsModule],
  template: `
    <main class="wrap">
      @if (auth.user(); as user) {
        <section class="card">
          <h1>Hello, {{ user.fullName }}</h1>
          <p class="muted">Your Reporti account · role: {{ user.role }}</p>
          <div class="row"><span>Email</span><strong>{{ user.email }}</strong></div>
          <div class="row"><span>Phone</span><strong>{{ user.phone || '—' }}</strong></div>
        </section>

        <section class="card">
          <h2>Edit profile</h2>
          <form (ngSubmit)="saveProfile()">
            <label>Full name <input name="fullName" [(ngModel)]="fullName" required /></label>
            <label>Phone <input name="phone" [(ngModel)]="phone" /></label>
            @if (profileErr()) { <p class="err">{{ profileErr() }}</p> }
            @if (profileMsg()) { <p class="ok">{{ profileMsg() }}</p> }
            <button type="submit" [disabled]="savingProfile()">Save changes</button>
          </form>
        </section>

        @if (isAgent()) {
          <section class="card">
            <h2>Assignment preferences</h2>
            <p class="muted">Choose the categories and sector you handle. You'll only be shown reports that match.</p>

            <div class="checkbox-group">
              <span class="group-label">Categories (select all that apply)</span>
              @for (c of categories(); track c.id) {
                <label class="checkbox-row">
                  <input type="checkbox" [checked]="selectedCategoryIds().includes(c.id)"
                         (change)="toggleCategory(c.id)" />
                  {{ c.name }}
                </label>
              } @empty {
                <span class="muted">No categories exist yet — ask an admin to create some.</span>
              }
            </div>

            <label style="margin-top: .75rem;">Sector
              <select name="sectorId" [ngModel]="selectedSectorId()" (ngModelChange)="selectedSectorId.set($event)">
                <option [ngValue]="null">— Any —</option>
                @for (s of sectors(); track s.id) {
                  <option [ngValue]="s.id">{{ s.name }}{{ s.zone ? ' (' + s.zone + ')' : '' }}</option>
                }
              </select>
            </label>

            @if (prefErr()) { <p class="err">{{ prefErr() }}</p> }
            @if (prefMsg()) { <p class="ok">{{ prefMsg() }}</p> }
            <button type="button" (click)="savePreferences()" [disabled]="savingPrefs()" style="margin-top: .75rem;">
              {{ savingPrefs() ? 'Saving…' : 'Save preferences' }}
            </button>
          </section>
        }

        <section class="card">
          <h2>Change password</h2>
          <form (ngSubmit)="changePassword()">
            <label>New password <input name="password" type="password" [(ngModel)]="password" /></label>
            <label>Confirm password <input name="confirm" type="password" [(ngModel)]="confirm" /></label>
            @if (passErr()) { <p class="err">{{ passErr() }}</p> }
            @if (passMsg()) { <p class="ok">{{ passMsg() }}</p> }
            <button type="submit" [disabled]="savingPass()">Update password</button>
          </form>
        </section>

        <button type="button" class="logout" (click)="auth.logout()">Log out</button>
      }
    </main>
  `,
  styles: [`
    :host { display: block; color: var(--text-main); }
    .wrap { max-width: 640px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; }
    .card { background: var(--surface-solid); border: 1px solid var(--border); border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); color: var(--text-main); }
    h1, h2 { margin: 0 0 .5rem; color: var(--text-main); }
    .muted { color: var(--muted); margin: 0 0 1rem; }
    .row { display: flex; justify-content: space-between; padding: .75rem 0; border-top: 1px solid var(--border); }
    form { display: grid; gap: .9rem; }
    label { display: grid; gap: .35rem; font-weight: 600; color: var(--text-main); }
    input, select { padding: .7rem .9rem; border: 1px solid var(--border); border-radius: 12px; font: inherit; background: var(--surface-solid); color: var(--text-main); }
    .checkbox-group { display: flex; flex-wrap: wrap; gap: .5rem 1.25rem; padding: .75rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); margin-top: .5rem; }
    .group-label { width: 100%; font-weight: 600; font-size: .85rem; color: var(--muted); }
    .checkbox-row { display: flex; align-items: center; gap: .35rem; font-weight: 500; }
    .checkbox-row input { width: auto; }
    button { padding: .75rem 1.25rem; border: 0; border-radius: 999px; background: var(--accent-red); color: #fff; font-weight: 700; cursor: pointer; width: fit-content; font: inherit; }
    button:disabled { opacity: .6; cursor: default; }
    .logout { background: #1c1c1a; }
    .err { color: var(--accent-red); margin: 0; }
    .ok { color: #22c55e; margin: 0; }
    :host-context(.dark-theme) button { color: #1a1a1a; }
    :host-context(.dark-theme) .logout { background: #50524c; color: #F7F4E8; }
  `],
})
export class AccountPage implements OnInit {
  protected readonly auth = inject(AuthApi);
  private readonly reportApi = inject(ReportApi);

  protected fullName = this.auth.user()?.fullName ?? '';
  protected phone = this.auth.user()?.phone ?? '';
  protected password = '';
  protected confirm = '';

  protected readonly savingProfile = signal(false);
  protected readonly profileMsg = signal('');
  protected readonly profileErr = signal('');

  protected readonly savingPass = signal(false);
  protected readonly passMsg = signal('');
  protected readonly passErr = signal('');

  protected readonly categories = signal<Category[]>([]);
  protected readonly sectors = signal<Sector[]>([]);
  protected readonly selectedCategoryIds = signal<number[]>([]);
  protected readonly selectedSectorId = signal<number | null>(null);
  protected readonly savingPrefs = signal(false);
  protected readonly prefMsg = signal('');
  protected readonly prefErr = signal('');

  ngOnInit(): void {
    const u = this.auth.user();
    if (u) {
      this.selectedCategoryIds.set([...u.categoryIds]);
      this.selectedSectorId.set(u.sectorId);
    }
    this.reportApi.getCategories().subscribe((c) => this.categories.set(c));
    this.reportApi.getSectors().subscribe((s) => this.sectors.set(s));
  }

  protected isAgent(): boolean {
    const r = this.auth.user()?.role;
    return r === 'agent' || r === 'sector_manager';
  }

  protected toggleCategory(id: number): void {
    const set = new Set(this.selectedCategoryIds());
    if (set.has(id)) set.delete(id); else set.add(id);
    this.selectedCategoryIds.set(Array.from(set));
  }

  protected async saveProfile(): Promise<void> {
    this.profileMsg.set(''); this.profileErr.set('');
    const name = this.fullName.trim();
    if (!name) { this.profileErr.set('Full name is required.'); return; }
    this.savingProfile.set(true);
    const { error } = await supabase.auth.updateUser({ data: { full_name: name, phone: this.phone.trim() || null } });
    this.savingProfile.set(false);
    if (error) this.profileErr.set(errorMessage(error));
    else this.profileMsg.set('Profile updated.');
  }

  protected async savePreferences(): Promise<void> {
    this.prefMsg.set(''); this.prefErr.set('');
    this.savingPrefs.set(true);
    const { error } = await supabase.auth.updateUser({
      data: {
        categoryIds: this.selectedCategoryIds(),
        sectorId: this.selectedSectorId(),
      },
    });
    this.savingPrefs.set(false);
    if (error) this.prefErr.set(errorMessage(error));
    else this.prefMsg.set('Assignment preferences saved. The change will apply on the next visit to Assignments.');
  }

  protected changePassword(): void {
    this.passMsg.set(''); this.passErr.set('');
    if (this.password.length < 6) { this.passErr.set('Password must be at least 6 characters.'); return; }
    if (this.password !== this.confirm) { this.passErr.set('Passwords do not match.'); return; }
    this.savingPass.set(true);
    this.auth.updatePassword(this.password).subscribe({
      next: (r) => { this.savingPass.set(false); this.passMsg.set(r.message); this.password = ''; this.confirm = ''; },
      error: (e) => { this.savingPass.set(false); this.passErr.set(errorMessage(e)); },
    });
  }
}