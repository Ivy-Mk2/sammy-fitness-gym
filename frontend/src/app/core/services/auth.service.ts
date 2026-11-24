import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, Rol, UsuarioSesion } from '../models';

export const CLAVE_TOKEN = 'token';

/**
 * Sesión del usuario. El estado se publica en un BehaviorSubject (usuario$) y se reconstruye
 * a partir del JWT: en localStorage solo se guarda el token (nunca datos del usuario).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly usuarioSubject = new BehaviorSubject<UsuarioSesion | null>(this.leerSesionGuardada());

  /** Usuario autenticado (null si no hay sesión). */
  readonly usuario$ = this.usuarioSubject.asObservable();
  readonly rol$: Observable<Rol | null> = this.usuario$.pipe(map((u) => u?.rol ?? null));
  /** Versión signal de usuario$ para las plantillas. */
  readonly usuario = toSignal(this.usuario$, { initialValue: this.usuarioSubject.value });

  get usuarioActual(): UsuarioSesion | null {
    const u = this.usuarioSubject.value;
    if (u && u.expira <= Date.now()) {
      this.cerrarSesionLocal();
      return null;
    }
    return u;
  }

  get token(): string | null {
    return this.usuarioActual ? leerToken() : null;
  }

  estaAutenticado(): boolean {
    return this.usuarioActual !== null;
  }

  tieneRol(...roles: Rol[]): boolean {
    const u = this.usuarioActual;
    return !!u && roles.includes(u.rol);
  }

  login(correo: string, password: string): Observable<UsuarioSesion> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { correo, password }).pipe(
      map((resp) => {
        const sesion = decodificarToken(resp.token);
        if (!sesion) {
          throw new Error('Token inválido recibido del servidor');
        }
        return { token: resp.token, sesion };
      }),
      tap(({ token, sesion }) => {
        guardarToken(token);
        this.usuarioSubject.next(sesion);
      }),
      map(({ sesion }) => sesion),
    );
  }

  /** Cierra la sesión y vuelve al login. */
  logout(motivo?: 'expirada'): void {
    this.cerrarSesionLocal();
    this.router.navigate(['/login'], motivo ? { queryParams: { sesion: motivo } } : {});
  }

  /** Ruta inicial según el rol. */
  rutaInicio(rol: Rol | undefined = this.usuarioActual?.rol): string {
    switch (rol) {
      case 'ADMINISTRADOR':
      case 'ENTRENADOR':
        return '/admin/clases';
      case 'RECEPCIONISTA':
        return '/admin/socios';
      default:
        return '/clases';
    }
  }

  private cerrarSesionLocal(): void {
    try {
      localStorage.removeItem(CLAVE_TOKEN);
    } catch {
      /* almacenamiento no disponible */
    }
    if (this.usuarioSubject.value !== null) {
      this.usuarioSubject.next(null);
    }
  }

  private leerSesionGuardada(): UsuarioSesion | null {
    const token = leerToken();
    const sesion = token ? decodificarToken(token) : null;
    if (token && !sesion) {
      try {
        localStorage.removeItem(CLAVE_TOKEN);
      } catch {
        /* ignorar */
      }
    }
    return sesion;
  }
}

function leerToken(): string | null {
  try {
    return localStorage.getItem(CLAVE_TOKEN);
  } catch {
    return null;
  }
}

function guardarToken(token: string): void {
  try {
    localStorage.setItem(CLAVE_TOKEN, token);
  } catch {
    /* almacenamiento no disponible: la sesión dura lo que la pestaña */
  }
}

/** Lee el payload del JWT (sin verificar la firma: eso lo hace el backend). Null si es inválido o expiró. */
export function decodificarToken(token: string): UsuarioSesion | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const binario = atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='));
    const json = new TextDecoder().decode(Uint8Array.from(binario, (c) => c.charCodeAt(0)));
    const p = JSON.parse(json) as { sub: string; id: number; rol: Rol; nombre: string; exp: number };
    const expira = p.exp * 1000;
    if (!p.sub || !p.rol || expira <= Date.now()) {
      return null;
    }
    return { id: p.id, correo: p.sub, nombre: p.nombre, rol: p.rol, expira };
  } catch {
    return null;
  }
}
