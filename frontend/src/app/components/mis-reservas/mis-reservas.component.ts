import { CurrencyPipe, DatePipe, NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink } from '@angular/router';
import { Membresia, Reserva, etiquetaDisciplina, iconoDisciplina } from '../../core/models';
import { MembresiaService } from '../../core/services/membresia.service';
import { ReservaService } from '../../core/services/reserva.service';
import { ToastService } from '../../core/services/toast.service';
import { hora, mensajeError, yaInicio } from '../../core/utils';
import { confirmar } from '../confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-mis-reservas',
  imports: [
    DatePipe,
    CurrencyPipe,
    NgTemplateOutlet,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h1 class="titulo">Mis reservas</h1>

    <!-- Membresía -->
    <mat-card appearance="outlined" class="membresia" [class]="'membresia ' + (membresia()?.estado ?? 'NINGUNA')">
      @if (membresia(); as m) {
        <div class="m-icono"><mat-icon>card_membership</mat-icon></div>
        <div class="m-datos">
          <span class="m-plan">Plan {{ m.planNombre }} · {{ m.precio | currency: 'PEN' : 'S/ ' }}</span>
          <span class="m-estado">{{ m.estado }}</span>
          <span class="m-fechas">
            Del {{ m.fechaInicio | date: 'd MMM y' }} al {{ m.fechaVencimiento | date: 'd MMM y' }}
            @if (m.estado === 'ACTIVA') {
              · <strong>{{ m.diasRestantes }} {{ m.diasRestantes === 1 ? 'día restante' : 'días restantes' }}</strong>
            }
          </span>
        </div>
        @if (m.estado === 'ACTIVA' && m.diasRestantes <= 3) {
          <p class="m-aviso"><mat-icon>warning</mat-icon>Tu membresía vence pronto. Renueva en recepción.</p>
        }
        @if (m.estado !== 'ACTIVA') {
          <p class="m-aviso"><mat-icon>info</mat-icon>Necesitas una membresía activa para reservar clases.</p>
        }
      } @else if (sinMembresia()) {
        <div class="m-icono"><mat-icon>card_membership</mat-icon></div>
        <div class="m-datos">
          <span class="m-plan">Sin membresía</span>
          <span class="m-fechas">Acércate a recepción para adquirir un plan.</span>
        </div>
      } @else {
        <mat-spinner diameter="28" />
      }
    </mat-card>

    <!-- Reservas -->
    @if (cargando()) {
      <div class="estado"><mat-spinner diameter="40" /></div>
    } @else {
      <mat-tab-group mat-stretch-tabs="false" animationDuration="150ms">
        <mat-tab [label]="'Próximas (' + proximas().length + ')'">
          @if (proximas().length === 0) {
            <div class="estado vacio">
              <mat-icon>event_available</mat-icon>
              <p>No tienes reservas próximas.</p>
              <a mat-flat-button routerLink="/clases">Ver clases</a>
            </div>
          } @else {
            <ul class="lista">
              @for (r of proximas(); track r.id) {
                <li>
                  <ng-container *ngTemplateOutlet="fila; context: { $implicit: r }" />
                </li>
              }
            </ul>
          }
        </mat-tab>
        <mat-tab [label]="'Historial (' + historial().length + ')'">
          @if (historial().length === 0) {
            <div class="estado vacio"><p>Aún no tienes historial.</p></div>
          } @else {
            <ul class="lista">
              @for (r of historial(); track r.id) {
                <li>
                  <ng-container *ngTemplateOutlet="fila; context: { $implicit: r }" />
                </li>
              }
            </ul>
          }
        </mat-tab>
      </mat-tab-group>
    }

    <ng-template #fila let-r>
      <mat-card appearance="outlined" class="reserva">
        <mat-icon class="r-icono" aria-hidden="true">{{ icono(r.disciplina) }}</mat-icon>
        <div class="r-datos">
          <span class="r-nombre">{{ r.claseNombre }}</span>
          <span class="r-detalle">
            {{ etiqueta(r.disciplina) }} · {{ r.entrenadorNombre }} ·
            <span class="fecha">{{ r.fecha | date: 'EEE d MMM' }}</span>, {{ hora(r.horaInicio) }}–{{ hora(r.horaFin) }}
          </span>
        </div>
        <span class="chip" [class]="'chip ' + r.estado">{{ r.estado }}</span>
        @if (r.cancelable) {
          <button mat-stroked-button class="cancelar" (click)="cancelar(r)" [disabled]="cancelandoId() === r.id">
            <mat-icon>close</mat-icon>Cancelar
          </button>
        }
      </mat-card>
    </ng-template>
  `,
  styleUrl: './mis-reservas.component.scss',
})
export class MisReservasComponent implements OnInit {
  private readonly reservaService = inject(ReservaService);
  private readonly membresiaService = inject(MembresiaService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  protected readonly reservas = signal<Reserva[]>([]);
  protected readonly membresia = signal<Membresia | null>(null);
  protected readonly sinMembresia = signal(false);
  protected readonly cargando = signal(true);
  protected readonly cancelandoId = signal<number | null>(null);

  protected readonly proximas = computed(() =>
    this.reservas()
      .filter((r) => r.estado === 'CONFIRMADA' && !yaInicio(r.fecha, r.horaInicio))
      .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio)),
  );
  protected readonly historial = computed(() => this.reservas().filter((r) => !this.proximas().includes(r)));

  protected readonly hora = hora;
  protected readonly etiqueta = etiquetaDisciplina;
  protected readonly icono = iconoDisciplina;

  ngOnInit(): void {
    this.cargarReservas();
    this.membresiaService.miMembresia().subscribe({
      next: (m) => this.membresia.set(m),
      error: (e) => {
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.sinMembresia.set(true);
        } else {
          this.toast.error(mensajeError(e));
        }
      },
    });
  }

  protected cancelar(r: Reserva): void {
    confirmar(this.dialog, {
      titulo: 'Cancelar reserva',
      mensaje: `¿Seguro que deseas cancelar tu reserva de "${r.claseNombre}"? El cupo quedará libre para otro socio.`,
      confirmar: 'Sí, cancelar',
      cancelar: 'No',
      peligro: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.cancelandoId.set(r.id);
      this.reservaService.cancelar(r.id).subscribe({
        next: () => {
          this.cancelandoId.set(null);
          this.toast.exito('Reserva cancelada. El cupo fue liberado.');
          this.cargarReservas();
        },
        error: (e) => {
          this.cancelandoId.set(null);
          this.toast.error(mensajeError(e));
        },
      });
    });
  }

  private cargarReservas(): void {
    this.reservaService.misReservas().subscribe({
      next: (rs) => {
        this.reservas.set(rs);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }
}
