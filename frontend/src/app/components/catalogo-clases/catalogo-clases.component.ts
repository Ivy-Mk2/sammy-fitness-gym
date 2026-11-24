import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Clase, Entrenador, FiltrosClase } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { ClaseService } from '../../core/services/clase.service';
import { ReservaService } from '../../core/services/reserva.service';
import { ToastService } from '../../core/services/toast.service';
import { hora, mensajeError } from '../../core/utils';
import { ClaseCardComponent } from '../clase-card/clase-card.component';
import { FiltrosComponent } from '../filtros/filtros.component';

/**
 * Catálogo de clases. El cliente reserva con un solo clic en "Reservar"
 * (login → catálogo → reservar: máximo 3 interacciones).
 */
@Component({
  selector: 'app-catalogo-clases',
  imports: [FiltrosComponent, ClaseCardComponent, MatProgressSpinnerModule, MatIconModule],
  template: `
    <header class="encabezado">
      <h1>Clases disponibles</h1>
      <p>
        @if (esCliente()) {
          Elige una clase y pulsa <strong>Reservar</strong>. Recibirás la confirmación por correo.
        } @else {
          Catálogo de clases programadas y su ocupación.
        }
      </p>
    </header>

    <app-filtros [entrenadores]="entrenadores()" (filtrosChange)="aplicarFiltros($event)" />

    <section class="resultados" aria-live="polite">
      @if (cargando()) {
        <div class="estado"><mat-spinner diameter="40" /></div>
      } @else if (clases().length === 0) {
        <div class="estado vacio">
          <mat-icon>event_busy</mat-icon>
          <p>No hay clases que coincidan con los filtros.</p>
        </div>
      } @else {
        <p class="conteo">{{ clases().length }} {{ clases().length === 1 ? 'clase' : 'clases' }}</p>
        <div class="grilla">
          @for (c of clases(); track c.id) {
            <app-clase-card
              [clase]="c"
              [modo]="esCliente() ? 'reservar' : 'lectura'"
              [reservada]="reservadas().has(c.id)"
              [procesando]="procesandoId() === c.id"
              (reservar)="reservar($event)"
            />
          }
        </div>
      }
    </section>
  `,
  styles: `
    .encabezado h1 { font: var(--mat-sys-headline-small); margin: 0 0 4px; }
    .encabezado p { margin: 0 0 20px; color: var(--mat-sys-on-surface-variant); }
    .resultados { margin-top: 20px; }
    .conteo { margin: 0 0 12px; color: var(--mat-sys-on-surface-variant); font: var(--mat-sys-label-large); }
    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .estado { display: grid; place-items: center; padding: 48px 0; }
    .vacio { color: var(--mat-sys-on-surface-variant); text-align: center; }
    .vacio mat-icon { font-size: 48px; width: 48px; height: 48px; }
  `,
})
export class CatalogoClasesComponent implements OnInit {
  private readonly claseService = inject(ClaseService);
  private readonly reservaService = inject(ReservaService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly clases = signal<Clase[]>([]);
  protected readonly entrenadores = signal<Entrenador[]>([]);
  protected readonly cargando = signal(true);
  protected readonly procesandoId = signal<number | null>(null);
  /** Ids de clases que el cliente ya tiene reservadas (CONFIRMADA o ASISTIDA). */
  protected readonly reservadas = signal<Set<number>>(new Set());
  protected readonly esCliente = computed(() => this.auth.usuario()?.rol === 'CLIENTE');

  private filtros: FiltrosClase = {};

  ngOnInit(): void {
    this.claseService.entrenadores().subscribe({
      next: (e) => this.entrenadores.set(e),
      error: () => this.entrenadores.set([]),
    });
    this.cargar();
    this.cargarReservadas();
  }

  protected aplicarFiltros(filtros: FiltrosClase): void {
    this.filtros = filtros;
    this.cargar();
  }

  protected reservar(clase: Clase): void {
    this.procesandoId.set(clase.id);
    this.reservaService.reservar(clase.id).subscribe({
      next: () => {
        this.procesandoId.set(null);
        this.toast.exito(`Reserva confirmada: ${clase.nombre} (${hora(clase.horaInicio)}). Te enviamos un correo.`);
        this.cargar(false);
        this.cargarReservadas();
      },
      error: (e) => {
        this.procesandoId.set(null);
        this.toast.error(mensajeError(e));
        this.cargar(false); // los cupos pueden haber cambiado
      },
    });
  }

  private cargar(mostrarSpinner = true): void {
    if (mostrarSpinner) this.cargando.set(true);
    this.claseService.listar(this.filtros).subscribe({
      next: (c) => {
        this.clases.set(c);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  private cargarReservadas(): void {
    if (!this.esCliente()) return;
    this.reservaService.misReservas().subscribe({
      next: (rs) => this.reservadas.set(new Set(rs.filter((r) => r.estado !== 'CANCELADA').map((r) => r.claseId))),
      error: () => this.reservadas.set(new Set()),
    });
  }
}
