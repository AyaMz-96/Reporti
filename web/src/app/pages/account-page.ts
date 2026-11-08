import { Component, inject } from '@angular/core';
import { AuthApi } from '../auth/auth-api';

@Component({
  selector: 'app-account-page',
  styleUrl: './auth-pages.css',
  template: `
    <main class="auth-page">
      <section class="auth-card">
        @if (auth.user(); as user) {
          <h1>Hello, {{ user.fullName }}</h1>
          <p class="sub">Your Reporti account</p>
          <div class="profile-row"><span>Email</span><strong>{{ user.email }}</strong></div>
          <div class="profile-row"><span>Phone</span><strong>{{ user.phone ?? '—' }}</strong></div>
          <p style="margin-top: 20px">
            <button class="auth-btn" type="button" (click)="auth.logout()">Log out</button>
          </p>
        }
      </section>
    </main>
  `,
})
export class AccountPage {
  protected readonly auth = inject(AuthApi);
}
