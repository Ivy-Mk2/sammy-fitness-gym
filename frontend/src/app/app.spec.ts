import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { CLAVE_TOKEN } from './core/services/auth.service';
import { CLAVE_TEMA } from './core/services/tema.service';
import { crearToken } from './testing';

describe('App', () => {
  async function render(): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => localStorage.clear());

  it('sin sesión no muestra la barra de navegación', async () => {
    const el = await render();
    expect(el.querySelector('app-navbar')).toBeNull();
  });

  it('con sesión muestra la barra con el nombre del usuario', async () => {
    localStorage.setItem(CLAVE_TOKEN, crearToken({ nombre: 'Rosa Quispe', rol: 'RECEPCIONISTA' }));
    const el = await render();
    expect(el.querySelector('app-navbar')?.textContent).toContain('Rosa Quispe');
  });

  it('aplica el tema oscuro guardado en localStorage', async () => {
    localStorage.setItem(CLAVE_TEMA, 'oscuro');
    await render();
    expect(document.documentElement.classList).toContain('tema-oscuro');
    expect(document.documentElement.style.colorScheme).toBe('dark');
  });
});
