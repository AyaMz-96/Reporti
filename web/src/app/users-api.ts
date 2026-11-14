import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { AuthApi, Role } from './auth/auth-api';
import { API_BASE_URL } from './config';

export interface AdminUser {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  suspended: boolean;
  createdAt: string;
  categoryIds: number[];
  sectorId: number | null;
}

export interface CreateUserBody {
  email: string;
  password: string;
  fullName: string;
  phone: string | null;
  role: Role;
  categoryIds?: number[];
  sectorId?: number | null;
}

export interface UpdateUserBody {
  fullName?: string;
  phone?: string;
  role?: Role;
  suspended?: boolean;
  categoryIds?: number[];
  sectorId?: number | null;
}

interface RawUser {
  id: string;
  email?: string;
  created_at?: string;
  banned_until?: string | null;
  user_metadata?: {
    full_name?: string;
    phone?: string | null;
    categoryIds?: Array<number | string> | null;
    categoryId?: number | string | null;
    sectorId?: number | string | null;
  };
  app_metadata?: { role?: string };
}

function num(v: unknown): number | null {
  if (v == null) return null;
  const n = typeof v === 'number' ? v : Number(v);
  return Number.isFinite(n) ? n : null;
}

function parseCategoryIds(meta: RawUser['user_metadata']): number[] {
  if (!meta) return [];
  const raw = meta.categoryIds;
  if (Array.isArray(raw)) {
    return raw.map(num).filter((n): n is number => n !== null);
  }
  const single = num(meta.categoryId);
  return single !== null ? [single] : [];
}

@Injectable({ providedIn: 'root' })
export class UsersApi {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthApi);
  private readonly url = `${API_BASE_URL}/admin/users`;

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.auth.token() ?? ''}` });
  }

  list(): Observable<AdminUser[]> {
    return this.http
      .get<{ users: RawUser[] }>(this.url, { headers: this.headers() })
      .pipe(map((r) => r.users.map((u) => this.toUser(u)).sort((a, b) => b.createdAt.localeCompare(a.createdAt))));
  }

  create(body: CreateUserBody): Observable<unknown> {
    return this.http.post(this.url, body, { headers: this.headers() });
  }

  update(id: string, body: UpdateUserBody): Observable<unknown> {
    return this.http.put(`${this.url}/${id}`, body, { headers: this.headers() });
  }

  remove(id: string): Observable<unknown> {
    return this.http.delete(`${this.url}/${id}`, { headers: this.headers() });
  }

  private toUser(u: RawUser): AdminUser {
    const role = u.app_metadata?.role;
    return {
      id: u.id,
      email: u.email ?? '',
      fullName: u.user_metadata?.full_name ?? '',
      phone: u.user_metadata?.phone ?? null,
      role: role === 'admin' || role === 'agent' || role === 'sector_manager' ? role : 'citizen',
      suspended: !!u.banned_until && new Date(u.banned_until) > new Date(),
      createdAt: u.created_at ?? '',
      categoryIds: parseCategoryIds(u.user_metadata),
      sectorId: num(u.user_metadata?.sectorId),
    };
  }
}
