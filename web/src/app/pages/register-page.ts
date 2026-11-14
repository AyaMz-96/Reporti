import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApi, errorMessage } from '../auth/auth-api';

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  return group.get('password')?.value === group.get('confirm')?.value ? null : { mismatch: true };
}

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './auth-pages.css',
  template: `
    <main class="auth-page">
      <section class="auth-card">
        <h1>Create your account</h1>
        <p class="sub">Report issues and track their progress.</p>

        @if (error()) { <p class="alert error">{{ error() }}</p> }

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label>
            Full name
            <input type="text" formControlName="fullName" autocomplete="name" />
            @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
              <span class="field-error">Full name is required.</span>
            }
          </label>
          <label>
            Email
            <input type="email" formControlName="email" autocomplete="email" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <span class="field-error">Enter a valid email.</span>
            }
          </label>
          <label>
            Phone (optional)
            <input type="tel" formControlName="phone" autocomplete="tel" />
          </label>
          <label>
            Password
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
            {{ loading() ? 'Creating account…' : 'Create account' }}
          </button>
        </form>

        <div class="auth-links">
          <span>Already registered?</span>
          <a routerLink="/login">Log in</a>
        </div>
      </section>
    </main>
  `,
})
export class RegisterPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthApi);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected readonly form = this.fb.group(
    {
      fullName: ['', [Validators.required, Validators.maxLength(150)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirm: ['', Validators.required],
    },
    { validators: matchPasswords },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { fullName, email, phone, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set('');
    this.auth.register({ fullName, email, phone: phone.trim() || null, password }).subscribe({
      next: () => this.router.navigateByUrl('/account'),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
