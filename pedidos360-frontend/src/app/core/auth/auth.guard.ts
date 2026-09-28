import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Bloquea detalle, carrito, checkout y pedidos cuando todavía no existe sesión. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService); const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  auth.open('login');
  return router.createUrlTree(['/']);
};
