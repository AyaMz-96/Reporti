import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Session, createClient } from '@supabase/supabase-js';
import { from, map } from 'rxjs';
import { SUPABASE_ANON_KEY, SUPABASE_URL } from '../supabase-config';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export type Role = 'citizen' | 'agent' | 'sector_manager' | 'admin';

export interface AuthUser {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  role: Role;
}

export function errorMessage(err: unknown): string {
  const msg = (err as { message?: string })?.message;
  return msg ? msg : 'Something went wrong. Please try again.';
}

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly session = signal<Session | null>(null);

  /** Se resout quand la session sauvegardee a ete lue (utile pour le guard). */
  readonly ready: Promise<void>;

  readonly token = computed(() => this.session()?.access_token ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly user = computed<AuthUser | null>(() => {
    const u = this.session()?.user;
    if (!u) return null;
    const rawRole = u.app_metadata?.['role'] as string | undefined;
    const role: Role =
      rawRole === 'admin' || rawRole === 'agent' || rawRole === 'sector_manager'
        ? rawRole
        : 'citizen';
    return {
      id: u.id,
      fullName: (u.user_metadata?.['full_name'] as string) ?? u.email ?? '',
      email: u.email ?? '',
      phone: (u.user_metadata?.['phone'] as string) ?? null,
      role,
    };
  });
  readonly isAdmin = computed(() => this.user()?.role === 'admin');
  readonly isAgent = computed(() => this.user()?.role === 'agent');
  readonly isSectorManager = computed(() => this.user()?.role === 'sector_manager');

  constructor(private readonly router: Router) {
    this.ready = supabase.auth.getSession().then(({ data }) => this.session.set(data.session));
    supabase.auth.onAuthStateChange((_event, s) => this.session.set(s));
  }

  register(b: { fullName: string; email: string; phone: string | null; password: string }) {
    return this.wrap(
      supabase.auth.signUp({
        email: b.email,
        password: b.password,
        options: { data: { full_name: b.fullName, phone: b.phone } },
      }),
    );
  }

  login(b: { email: string; password: string }) {
    return this.wrap(supabase.auth.signInWithPassword(b));
  }

  forgotPassword(email: string) {
    return this.wrap(
      supabase.auth.resetPasswordForEmail(email, {
        redirectTo: `${window.location.origin}/reset-password`,
      }),
    ).pipe(map(() => ({ message: 'If an account exists for this email, a reset link has been sent.' })));
  }

  updatePassword(password: string) {
    return this.wrap(supabase.auth.updateUser({ password })).pipe(
      map(() => ({ message: 'Your password has been updated.' })),
    );
  }

  async logout(): Promise<void> {
    await supabase.auth.signOut();
    this.router.navigateByUrl('/');
  }

  private wrap<T extends { data: unknown; error: Error | null }>(p: PromiseLike<T>) {
    return from(
      Promise.resolve(p).then((r) => {
        if (r.error) throw r.error;
        return r.data as T['data'];
      }),
    );
  }
}