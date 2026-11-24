import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * Agrega "Authorization: Bearer <token>" a las peticiones hacia la API y, si el backend
 * responde 401 (token ausente, inválido o expirado), cierra la sesión y redirige a /login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const toast = inject(ToastService);

  const esApi = req.url.startsWith(environment.apiUrl);
  const esLogin = req.url.endsWith('/auth/login');
  const token = auth.token;

  const peticion = esApi && token && !esLogin ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && esApi && !esLogin) {
        if (auth.estaAutenticado() || token) {
          toast.info('Su sesión expiró. Inicie sesión nuevamente.');
        }
        auth.logout('expirada');
      }
      return throwError(() => error);
    }),
  );
};
