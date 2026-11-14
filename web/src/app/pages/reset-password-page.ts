import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthApi, errorMessage } from '../auth/auth-api';

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  return group.get('password')?.value === group.get('confirm')?.value ? null : { mismatch: true };
}

@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth-pages.css',
  template: `
    <main class="auth-page">
      <section class="auth-card">
        <h1>Choose a new password</h1>

        @if (checking()) {
          <p class="sub">Checking your link…</p>
        } @else if (!valid()) {
          <p class="alert error">This reset link is invalid or has expired.</p>
          <div class="auth-links"><a routerLink="/forgot-password">Request a new link</a></div>
        } @else if (success()) {
          <p class="alert success">{{ success() }}</p>
          <div class="auth-links"><a routerLink="/account">Go to my account</a></div>
        } @else {
          <p class="sub">Your new password must have at least 8 characters.</p>
          @if (error()) { <p class="alert error">{{ error() }}</p> }

          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <label>
              New password
              <input type="password" formControlName="password" autocomplete="new-password" />
              @if (form.controls.password.touched && form.controls.password.invalid) {
                <span class="field-error">At least 8 characters.</span>
              }
            </label>
            <label>
              Confirm password
              <input type="password" formControlName="confirm" autocomplete="new-password" />
              @if (form.controls.confirm.touched && form.hasError('mismatch')) {
                <span class="field-error">Passwords do not match.</span>
              }
            </label>
            <button class="auth-btn" type="submit" [disabled]="loading()">
              {{ loading() ? 'Saving…' : 'Update password' }}
            </button>
          </form>
        }
      </section>
    </main>
  `,
})
export class ResetPasswordPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthApi);

  protected readonly checking = signal(true);
  protected readonly valid = signal(false);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');

  protected readonly form = this.fb.group(
    {
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirm: ['', Validators.required],
    },
    { validators: matchPasswords },
  );

  constructor() {
    this.auth.ready.then(() => {
      this.valid.set(this.auth.isLoggedIn());
      this.checking.set(false);
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.updatePassword(this.form.getRawValue().password).subscribe({
      next: (r) => this.success.set(r.message),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
