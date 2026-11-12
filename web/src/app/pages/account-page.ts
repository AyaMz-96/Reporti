import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthApi, errorMessage, supabase } from '../auth/auth-api';

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
            <label>Full name
              <input name="fullName" [(ngModel)]="fullName" required />
            </label>
            <label>Phone
              <input name="phone" [(ngModel)]="phone" />
            </label>
            @if (profileErr()) { <p class="err">{{ profileErr() }}</p> }
            @if (profileMsg()) { <p class="ok">{{ profileMsg() }}</p> }
            <button type="submit" [disabled]="savingProfile()">Save changes</button>
          </form>
        </section>

        <section class="card">
          <h2>Change password</h2>
          <form (ngSubmit)="changePassword()">
            <label>New password
              <input name="password" type="password" [(ngModel)]="password" />
            </label>
            <label>Confirm password
              <input name="confirm" type="password" [(ngModel)]="confirm" />
            </label>
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
    .wrap { max-width: 640px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; }
    .card { background: #fffdf8; border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); color: #1c1c1a; }
    h1, h2 { margin: 0 0 .5rem; }
    .muted { color: #6b6b66; margin: 0 0 1rem; }
    .row { display: flex; justify-content: space-between; padding: .75rem 0; border-top: 1px solid #e6e1d6; }
    form { display: grid; gap: .9rem; }
    label { display: grid; gap: .35rem; font-weight: 600; }
    input { padding: .7rem .9rem; border: 1px solid #d6d0c2; border-radius: 12px; font: inherit; }
    button { padding: .75rem 1.25rem; border: 0; border-radius: 999px; background: #d92f2f; color: #fff; font-weight: 700; cursor: pointer; width: fit-content; }
    button:disabled { opacity: .6; cursor: default; }
    .logout { background: #1c1c1a; }
    .err { color: #b3261e; margin: 0; }
    .ok { color: #1b7f3b; margin: 0; }
  `],
})
export class AccountPage {
  protected readonly auth = inject(AuthApi);

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

  protected async saveProfile(): Promise<void> {
    this.profileMsg.set('');
    this.profileErr.set('');
    const name = this.fullName.trim();
    if (!name) {
      this.profileErr.set('Full name is required.');
      return;
    }
    this.savingProfile.set(true);
    const { error } = await supabase.auth.updateUser({
      data: { full_name: name, phone: this.phone.trim() || null },
    });
    this.savingProfile.set(false);
    if (error) this.profileErr.set(errorMessage(error));
    else this.profileMsg.set('Profile updated.');
  }

  protected changePassword(): void {
    this.passMsg.set('');
    this.passErr.set('');
    if (this.password.length < 6) {
      this.passErr.set('Password must be at least 6 characters.');
      return;
    }
    if (this.password !== this.confirm) {
      this.passErr.set('Passwords do not match.');
      return;
    }
    this.savingPass.set(true);
    this.auth.updatePassword(this.password).subscribe({
      next: (r) => {
        this.savingPass.set(false);
        this.passMsg.set(r.message);
        this.password = '';
        this.confirm = '';
      },
      error: (e) => {
        this.savingPass.set(false);
        this.passErr.set(errorMessage(e));
      },
    });
  }
}