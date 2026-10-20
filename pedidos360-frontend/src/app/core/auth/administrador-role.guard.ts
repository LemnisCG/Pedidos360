import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Se usa después de MsalGuard: la sesión ya existe y aquí solo se exige el App Role "Administrador".
 * Es una ayuda visual; la autorización real la aplica el API Gateway, que responde 403.
 */
export const administradorRoleGuard: CanActivateFn = () => {
  const auth = inject(AuthService); const router = inject(Router);
  return auth.hasAdministradorRole() ? true : router.createUrlTree(['/']);
};
