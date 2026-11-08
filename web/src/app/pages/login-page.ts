import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApi, errorMessage } from '../auth/auth-api';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth-pages.css',
  template: `
    <main class="auth-page">
      <section class="auth-card">
        <h1>Welcome back</h1>
        <p class="sub">Log in to follow your reports.</p>

        @if (error()) { <p class="alert error">{{ error() }}</p> }

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label>
            Email
            <input type="email" formControlName="email" autocomplete="email" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <span class="field-error">Enter a valid email.</span>
            }
          </label>
          <label>
            Password
            <input type="password" formControlName="password" autocomplete="current-password" />
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <span class="field-error">Password is required.</span>
            }
          </label>
          <button class="auth-btn" type="submit" [disabled]="loading()">
            {{ loading() ? 'Logging in…' : 'Log in' }}
          </button>
        </form>

        <div class="auth-links">
          <a routerLink="/forgot-password">Forgot password?</a>
          <a routerLink="/register">Create an account</a>
        </div>
      </section>
    </main>
  `,
})
export class LoginPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthApi);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/account'),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
