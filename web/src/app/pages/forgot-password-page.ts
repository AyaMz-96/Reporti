import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthApi, errorMessage } from '../auth/auth-api';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth-pages.css',
  template: `
    <main class="auth-page">
      <section class="auth-card">
        <h1>Forgot your password?</h1>
        <p class="sub">Enter your email and we will send you a reset link.</p>

        @if (error()) { <p class="alert error">{{ error() }}</p> }
        @if (success()) { <p class="alert success">{{ success() }}</p> }

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label>
            Email
            <input type="email" formControlName="email" autocomplete="email" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <span class="field-error">Enter a valid email.</span>
            }
          </label>
          <button class="auth-btn" type="submit" [disabled]="loading()">
            {{ loading() ? 'Sending…' : 'Send reset link' }}
          </button>
        </form>

        <div class="auth-links">
          <a routerLink="/login">Back to login</a>
        </div>
      </section>
    </main>
  `,
})
export class ForgotPasswordPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthApi);

  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: (r) => {
        this.success.set(r.message);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
