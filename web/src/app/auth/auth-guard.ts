import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthApi } from './auth-api';

export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthApi);
  const router = inject(Router);
  await auth.ready;
  return auth.isLoggedIn() ? true : router.createUrlTree(['/login']);
};
