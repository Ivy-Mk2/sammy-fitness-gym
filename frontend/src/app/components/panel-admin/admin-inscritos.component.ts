import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { Clase, Inscrito } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { ClaseService } from '../../core/services/clase.service';
import { ReservaService } from '../../core/services/reserva.service';
import { ToastService } from '../../core/services/toast.service';
import { hora, hoyIso, mensajeError } from '../../core/utils';

/** Subvista "Inscritos": lista de una clase y registro de asistencia (ADMINISTRADOR y ENTRENADOR). */
@Component({
  selector: 'app-admin-inscritos',
  imports: [DatePipe, MatFormFieldModule, MatSelectModule, MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="barra-acciones">
      <h2>Inscritos por clase</h2>
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="selector">
        <mat-label>Clase</mat-label>
        <mat-select [value]="seleccionadaId()" (valueChange)="seleccionar($event)">
          @for (c of clases(); track c.id) {
            <mat-option [value]="c.id">
              {{ c.fecha | date: 'EEE d MMM' }} {{ h(c.horaInicio) }} · {{ c.nombre }} ({{ c.cuposOcupados }}/{{ c.aforoMaximo }})
            </mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>

    @if (!seleccionada()) {
      <p class="estado-vacio">Seleccione una clase para ver sus inscritos.</p>
    } @else if (cargando()) {
      <div class="cargando"><mat-spinner diameter="40" /></div>
    } @else {
      <p class="secundario resumen">
        {{ seleccionada()!.nombre }} · {{ seleccionada()!.entrenadorNombre }} ·
        {{ inscritos().length }}/{{ seleccionada()!.aforoMaximo }} inscritos
        @if (puedeMarcar() && !esDiaDeClase()) { · La asistencia se registra desde el día de la clase }
      </p>
      @if (inscritos().length === 0) {
        <p class="estado-vacio">Aún no hay inscritos en esta clase.</p>
      } @else {
        <div class="tabla-scroll">
          <table mat-table [dataSource]="inscritos()">
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Cliente</th>
              <td mat-cell *matCellDef="let i">{{ i.nombreCompleto }}<br /><span class="secundario">DNI {{ i.dni }}</span></td>
            </ng-container>
            <ng-container matColumnDef="contacto">
              <th mat-header-cell *matHeaderCellDef>Contacto</th>
              <td mat-cell *matCellDef="let i">{{ i.telefono }}<br /><span class="secundario">{{ i.correo }}</span></td>
            </ng-container>
            <ng-container matColumnDef="estado">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let i"><span class="chip" [class]="'chip ' + i.estado">{{ i.estado }}</span></td>
            </ng-container>
            <ng-container matColumnDef="acciones">
              <th mat-header-cell *matHeaderCellDef><span class="cdk-visually-hidden">Asistencia</span></th>
              <td mat-cell *matCellDef="let i" class="acciones-celda">
                @if (puedeMarcar()) {
                  @if (i.estado === 'ASISTIDA') {
                    <button mat-stroked-button (click)="asistencia(i, false)" [disabled]="procesandoId() === i.reservaId">
                      <mat-icon>undo</mat-icon>Quitar asistencia
                    </button>
                  } @else {
                    <button mat-flat-button (click)="asistencia(i, true)"
                            [disabled]="!esDiaDeClase() || procesandoId() === i.reservaId">
                      <mat-icon>how_to_reg</mat-icon>Marcar asistencia
                    </button>
                  }
                }
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columnas"></tr>
            <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
          </table>
        </div>
      }
    }
  `,
  styleUrl: './admin.scss',
  styles: `
    .selector { flex: 1; min-width: 260px; max-width: 520px; }
    .resumen { margin: 0 0 12px; }
  `,
})
export class AdminInscritosComponent implements OnInit {
  private readonly claseService = inject(ClaseService);
  private readonly reservaService = inject(ReservaService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  /** Query param ?claseId= (enlazado por withComponentInputBinding). */
  readonly claseId = input<string | undefined>();

  protected readonly columnas = ['nombre', 'contacto', 'estado', 'acciones'];
  protected readonly h = hora;

  protected readonly clases = signal<Clase[]>([]);
  protected readonly inscritos = signal<Inscrito[]>([]);
  protected readonly cargando = signal(false);
  protected readonly procesandoId = signal<number | null>(null);
  protected readonly seleccionadaId = signal<number | null>(null);

  protected readonly seleccionada = computed(() => this.clases().find((c) => c.id === this.seleccionadaId()) ?? null);
  protected readonly puedeMarcar = computed(() => ['ADMINISTRADOR', 'ENTRENADOR'].includes(this.auth.usuario()?.rol ?? ''));
  protected readonly esDiaDeClase = computed(() => {
    const c = this.seleccionada();
    return !!c && c.fecha <= hoyIso();
  });

  constructor() {
    effect(() => {
      const id = Number(this.claseId());
      if (id && id !== this.seleccionadaId()) {
        this.seleccionadaId.set(id);
        this.cargarInscritos(id);
      }
    });
  }

  ngOnInit(): void {
    const u = this.auth.usuarioActual;
    const filtro = u?.rol === 'ENTRENADOR' ? { entrenadorId: u.id } : {};
    this.claseService.listar(filtro).subscribe({
      next: (c) => this.clases.set(c),
      error: (e) => this.toast.error(mensajeError(e)),
    });
  }

  protected seleccionar(id: number): void {
    this.router.navigate([], { queryParams: { claseId: id }, replaceUrl: true });
  }

  protected asistencia(i: Inscrito, asistio: boolean): void {
    this.procesandoId.set(i.reservaId);
    this.reservaService.marcarAsistencia(i.reservaId, asistio).subscribe({
      next: () => {
        this.procesandoId.set(null);
        this.toast.exito(asistio ? `Asistencia registrada: ${i.nombreCompleto}` : 'Asistencia retirada');
        const claseId = this.seleccionadaId();
        if (claseId) this.cargarInscritos(claseId);
      },
      error: (e) => {
        this.procesandoId.set(null);
        this.toast.error(mensajeError(e));
      },
    });
  }

  private cargarInscritos(claseId: number): void {
    this.cargando.set(true);
    this.claseService.inscritos(claseId).subscribe({
      next: (i) => {
        this.inscritos.set(i);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.inscritos.set([]);
        this.toast.error(mensajeError(e));
      },
    });
  }
}
