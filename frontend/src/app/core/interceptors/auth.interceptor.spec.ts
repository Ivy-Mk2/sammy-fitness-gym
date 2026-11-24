import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { crearToken } from '../../testing';
import { CLAVE_TOKEN } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;
  const api = environment.apiUrl;

  function configurar(): void {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => backend.verify());

  it('agrega Authorization: Bearer <token> a las peticiones de la API', () => {
    const token = crearToken();
    localStorage.setItem(CLAVE_TOKEN, token);
    configurar();

    http.get(`${api}/clases`).subscribe();

    expect(backend.expectOne(`${api}/clases`).request.headers.get('Authorization')).toBe(`Bearer ${token}`);
  });

  it('no agrega el token al login ni a otros dominios', () => {
    localStorage.setItem(CLAVE_TOKEN, crearToken());
    configurar();

    http.post(`${api}/auth/login`, {}).subscribe();
    http.get('https://otro-sitio.test/datos').subscribe();

    expect(backend.expectOne(`${api}/auth/login`).request.headers.has('Authorization')).toBe(false);
    expect(backend.expectOne('https://otro-sitio.test/datos').request.headers.has('Authorization')).toBe(false);
  });

  it('ante un 401 cierra la sesión y redirige a /login', () => {
    localStorage.setItem(CLAVE_TOKEN, crearToken());
    configurar();
    let error: unknown;

    http.get(`${api}/reservas/mis-reservas`).subscribe({ error: (e) => (error = e) });
    backend.expectOne(`${api}/reservas/mis-reservas`).flush({ codigo: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(error).toBeTruthy();
    expect(localStorage.getItem(CLAVE_TOKEN)).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { sesion: 'expirada' } });
  });

  it('un 401 del propio login (credenciales incorrectas) no redirige', () => {
    configurar();

    http.post(`${api}/auth/login`, {}).subscribe({ error: () => undefined });
    backend.expectOne(`${api}/auth/login`).flush({ mensaje: 'Credenciales incorrectas' }, { status: 401, statusText: '' });

    expect(navegar).not.toHaveBeenCalled();
  });

  it('otros errores (403, 409) no cierran la sesión', () => {
    localStorage.setItem(CLAVE_TOKEN, crearToken());
    configurar();

    http.post(`${api}/reservas`, {}).subscribe({ error: () => undefined });
    backend.expectOne(`${api}/reservas`).flush({ mensaje: 'aforo completo' }, { status: 409, statusText: '' });

    expect(localStorage.getItem(CLAVE_TOKEN)).not.toBeNull();
    expect(navegar).not.toHaveBeenCalled();
  });
});
