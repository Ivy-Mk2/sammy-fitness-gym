import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Clase } from '../../core/models';
import { ClaseCardComponent, nivelCupos } from './clase-card.component';

registerLocaleData(localeEsPe);

function clase(parcial: Partial<Clase> = {}): Clase {
  return {
    id: 1,
    nombre: 'Spinning Power',
    disciplina: 'SPINNING',
    entrenadorId: 3,
    entrenadorNombre: 'Carlos Ramos',
    fecha: '2099-01-15',
    horaInicio: '07:00:00',
    horaFin: '08:00:00',
    aforoMaximo: 20,
    cuposOcupados: 14,
    cuposDisponibles: 6,
    descripcion: null,
    ...parcial,
  };
}

describe('ClaseCardComponent', () => {
  let fixture: ComponentFixture<ClaseCardComponent>;

  async function render(c: Clase, extra: { reservada?: boolean } = {}): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [ClaseCardComponent],
      providers: [{ provide: LOCALE_ID, useValue: 'es-PE' }],
    }).compileComponents();
    fixture = TestBed.createComponent(ClaseCardComponent);
    fixture.componentRef.setInput('clase', c);
    fixture.componentRef.setInput('modo', 'reservar');
    fixture.componentRef.setInput('reservada', extra.reservada ?? false);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const boton = (el: HTMLElement) => el.querySelector('.acciones button') as HTMLButtonElement;

  it('muestra los cupos como texto "14/20 cupos" y la hora sin segundos', async () => {
    const el = await render(clase());
    expect(el.querySelector('.texto-cupos')?.textContent?.trim()).toBe('14/20 cupos');
    expect(el.textContent).toContain('07:00 – 08:00');
  });

  it('con cupos libres el botón Reservar está habilitado y emite la clase', async () => {
    const el = await render(clase());
    let emitida: Clase | undefined;
    fixture.componentInstance.reservar.subscribe((c) => (emitida = c));

    expect(boton(el).textContent).toContain('Reservar');
    expect(boton(el).disabled).toBe(false);
    boton(el).click();
    expect(emitida?.id).toBe(1);
  });

  it('sin cupos muestra "Completo" deshabilitado y la barra en rojo', async () => {
    const el = await render(clase({ cuposOcupados: 20, cuposDisponibles: 0 }));
    expect(boton(el).textContent).toContain('Completo');
    expect(boton(el).disabled).toBe(true);
    expect(el.querySelector('.relleno')?.classList).toContain('completo');
  });

  it('si el cliente ya la reservó muestra "Reservada" deshabilitado', async () => {
    const el = await render(clase(), { reservada: true });
    expect(boton(el).textContent).toContain('Reservada');
    expect(boton(el).disabled).toBe(true);
  });

  it('nivel de cupos: verde < 70 %, ámbar < 100 %, rojo lleno', () => {
    expect(nivelCupos(5, 20)).toBe('disponible');
    expect(nivelCupos(14, 20)).toBe('pocos');
    expect(nivelCupos(20, 20)).toBe('completo');
  });
});
