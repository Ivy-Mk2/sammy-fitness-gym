import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { crearToken } from '../../testing';
import { Rol } from '../models';
import { CLAVE_TOKEN } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';

describe('AuthGuard y RoleGuard', () => {
  function configurar(rol?: Rol): void {
    localStorage.clear();
    if (rol) localStorage.setItem(CLAVE_TOKEN, crearToken({ rol }));
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
  }

  const estado = (url: string) => ({ url }) as RouterStateSnapshot;
  const ruta = (roles: Rol[]) => ({ data: { roles } }) as unknown as ActivatedRouteSnapshot;
  const url = (r: unknown) => TestBed.inject(Router).serializeUrl(r as UrlTree);

  it('AuthGuard: sin sesión redirige a /login conservando la ruta', () => {
    configurar();
    const r = TestBed.runInInjectionContext(() => authGuard(ruta([]), estado('/mis-reservas')));
    expect(url(r)).toBe('/login?volver=%2Fmis-reservas');
  });

  it('AuthGuard: con sesión deja pasar', () => {
    configurar('CLIENTE');
    expect(TestBed.runInInjectionContext(() => authGuard(ruta([]), estado('/clases')))).toBe(true);
  });

  it('RoleGuard: deja pasar si el rol está permitido', () => {
    configurar('RECEPCIONISTA');
    const r = TestBed.runInInjectionContext(() => roleGuard(ruta(['ADMINISTRADOR', 'RECEPCIONISTA']), estado('/admin/socios')));
    expect(r).toBe(true);
  });

  it('RoleGuard: un cliente no entra al panel y vuelve a su inicio', () => {
    configurar('CLIENTE');
    const r = TestBed.runInInjectionContext(() => roleGuard(ruta(['ADMINISTRADOR']), estado('/admin')));
    expect(url(r)).toBe('/clases');
  });

  it('RoleGuard: un entrenador no entra a socios', () => {
    configurar('ENTRENADOR');
    const r = TestBed.runInInjectionContext(() => roleGuard(ruta(['ADMINISTRADOR', 'RECEPCIONISTA']), estado('/admin/socios')));
    expect(url(r)).toBe('/admin/clases');
  });
});
