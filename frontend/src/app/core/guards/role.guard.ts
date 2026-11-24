import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * RoleGuard: exige que el rol del usuario esté en route.data['roles'].
 * Uso: { path: 'admin', canActivate: [authGuard, roleGuard], data: { roles: ['ADMINISTRADOR'] } }
 */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const roles = (route.data['roles'] as Rol[] | undefined) ?? [];
  const usuario = auth.usuarioActual;
  if (!usuario) {
    return router.createUrlTree(['/login']);
  }
  if (roles.length === 0 || roles.includes(usuario.rol)) {
    return true;
  }
  toast.error('No tiene permiso para acceder a esa sección');
  return router.createUrlTree([auth.rutaInicio()]);
};
