import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthApi, Role } from './auth-api';

/** Allows access only to logged-in users having one of the given roles. */
export function roleGuard(...allowed: Role[]): CanActivateFn {
  return async () => {
    const auth = inject(AuthApi);
    const router = inject(Router);
    await auth.ready;
    if (!auth.isLoggedIn()) return router.createUrlTree(['/login']);
    const role = auth.user()?.role;
    return role && allowed.includes(role) ? true : router.createUrlTree(['/']);
  };
}

export const adminGuard: CanActivateFn = roleGuard('admin');
export const agentGuard: CanActivateFn = roleGuard('agent', 'admin');
export const sectorManagerGuard: CanActivateFn = roleGuard('sector_manager', 'admin');