import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { crearToken } from '../../testing';
import { AuthService, CLAVE_TOKEN, decodificarToken } from './auth.service';

describe('AuthService', () => {
  let http: HttpTestingController;

  function crearServicio(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http?.verify());

  it('decodifica el payload del JWT, incluidos nombres con tildes', () => {
    const sesion = decodificarToken(crearToken({ rol: 'ENTRENADOR', nombre: 'Lucía Núñez' }));
    expect(sesion).toMatchObject({ id: 7, correo: 'ana@correo.test', rol: 'ENTRENADOR', nombre: 'Lucía Núñez' });
  });

  it('considera inválidos los tokens expirados o mal formados', () => {
    expect(decodificarToken(crearToken({ expiraEnSeg: -10 }))).toBeNull();
    expect(decodificarToken('no-es-un-jwt')).toBeNull();
  });

  it('login guarda solo el token en localStorage y publica el usuario en el BehaviorSubject', async () => {
    const auth = crearServicio();
    const token = crearToken({ rol: 'CLIENTE' });

    const resultado = firstValueFrom(auth.login('ana@correo.test', 'secreta'));
    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ correo: 'ana@correo.test', password: 'secreta' });
    req.flush({ token, rol: 'CLIENTE', nombre: 'Ana Pérez' });

    const usuario = await resultado;
    expect(usuario.rol).toBe('CLIENTE');
    expect(await firstValueFrom(auth.usuario$)).toMatchObject({ nombre: 'Ana Pérez' });
    expect(localStorage.getItem(CLAVE_TOKEN)).toBe(token);
    expect(Object.keys(localStorage)).toEqual([CLAVE_TOKEN]);
  });

  it('restaura la sesión desde un token guardado válido', () => {
    localStorage.setItem(CLAVE_TOKEN, crearToken({ rol: 'ADMINISTRADOR' }));
    const auth = crearServicio();
    expect(auth.estaAutenticado()).toBe(true);
    expect(auth.tieneRol('ADMINISTRADOR')).toBe(true);
    expect(auth.tieneRol('CLIENTE')).toBe(false);
  });

  it('descarta un token guardado que ya expiró', () => {
    localStorage.setItem(CLAVE_TOKEN, crearToken({ expiraEnSeg: -5 }));
    const auth = crearServicio();
    expect(auth.estaAutenticado()).toBe(false);
    expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
  });

  it('logout borra el token, emite null y navega a /login', async () => {
    localStorage.setItem(CLAVE_TOKEN, crearToken());
    const auth = crearServicio();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    auth.logout();

    expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
    expect(await firstValueFrom(auth.usuario$)).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login'], {});
  });

  it('ruta de inicio según el rol', () => {
    const auth = crearServicio();
    expect(auth.rutaInicio('CLIENTE')).toBe('/clases');
    expect(auth.rutaInicio('ADMINISTRADOR')).toBe('/admin/clases');
    expect(auth.rutaInicio('ENTRENADOR')).toBe('/admin/clases');
    expect(auth.rutaInicio('RECEPCIONISTA')).toBe('/admin/socios');
  });
});
