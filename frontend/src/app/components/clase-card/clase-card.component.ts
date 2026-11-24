import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Clase, etiquetaDisciplina, iconoDisciplina } from '../../core/models';
import { hora, yaInicio } from '../../core/utils';

export type NivelCupos = 'disponible' | 'pocos' | 'completo';

/** Nivel de ocupación para el color de la barra: verde < 70 %, ámbar < 100 %, rojo = lleno. */
export function nivelCupos(ocupados: number, aforo: number): NivelCupos {
  if (ocupados >= aforo) return 'completo';
  return ocupados / aforo >= 0.7 ? 'pocos' : 'disponible';
}

/** Tarjeta de una clase con barra de cupos de color y texto "14/20 cupos". */
@Component({
  selector: 'app-clase-card',
  imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <mat-card appearance="outlined" class="tarjeta" [class.finalizada]="finalizada()">
      <div class="cabecera">
        <span class="disciplina">
          <mat-icon aria-hidden="true">{{ icono() }}</mat-icon>{{ etiqueta() }}
        </span>
        <span class="fecha">{{ clase().fecha | date: 'EEE d MMM' }}</span>
      </div>

      <h3 class="titulo">{{ clase().nombre }}</h3>
      <p class="dato"><mat-icon aria-hidden="true">schedule</mat-icon>{{ inicio() }} – {{ fin() }}</p>
      <p class="dato"><mat-icon aria-hidden="true">person</mat-icon>{{ clase().entrenadorNombre }}</p>
      @if (clase().descripcion) {
        <p class="descripcion">{{ clase().descripcion }}</p>
      }

      <div class="cupos">
        <div
          class="barra"
          role="progressbar"
          [attr.aria-valuenow]="clase().cuposOcupados"
          aria-valuemin="0"
          [attr.aria-valuemax]="clase().aforoMaximo"
          [attr.aria-label]="textoCupos()"
        >
          <div class="relleno" [class]="'relleno ' + nivel()" [style.width.%]="porcentaje()"></div>
        </div>
        <span class="texto-cupos" [class]="'texto-cupos ' + nivel()">{{ textoCupos() }}</span>
      </div>

      @if (modo() === 'reservar') {
        <div class="acciones">
          @if (reservada()) {
            <button mat-stroked-button disabled><mat-icon>check</mat-icon>Reservada</button>
          } @else if (finalizada()) {
            <button mat-stroked-button disabled>Finalizada</button>
          } @else if (nivel() === 'completo') {
            <button mat-stroked-button disabled class="completo"><mat-icon>block</mat-icon>Completo</button>
          } @else {
            <button mat-flat-button (click)="reservar.emit(clase())" [disabled]="procesando()">
              @if (procesando()) {
                <mat-spinner diameter="18" class="girando" />
              } @else {
                <mat-icon>event_available</mat-icon>
              }
              {{ procesando() ? 'Reservando…' : 'Reservar' }}
            </button>
          }
        </div>
      }
    </mat-card>
  `,
  styles: `
    .tarjeta { height: 100%; display: flex; flex-direction: column; padding: 16px; box-sizing: border-box; gap: 6px; }
    .finalizada { opacity: 0.6; }
    .cabecera { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
    .disciplina {
      display: inline-flex; align-items: center; gap: 4px; padding: 2px 10px 2px 6px; border-radius: 999px;
      background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container);
      font: var(--mat-sys-label-medium);
    }
    .disciplina mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .fecha { font: var(--mat-sys-label-large); color: var(--mat-sys-on-surface-variant); text-transform: capitalize; }
    .titulo { font: var(--mat-sys-title-medium); margin: 6px 0 2px; }
    .dato { display: flex; align-items: center; gap: 6px; margin: 0; color: var(--mat-sys-on-surface-variant); }
    .dato mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .descripcion { margin: 4px 0 0; font: var(--mat-sys-body-small); color: var(--mat-sys-on-surface-variant); }
    .cupos { display: flex; align-items: center; gap: 10px; margin-top: auto; padding-top: 10px; }
    .barra { flex: 1; height: 8px; border-radius: 999px; background: var(--mat-sys-surface-container-highest); overflow: hidden; }
    .relleno { height: 100%; border-radius: 999px; transition: width 0.3s; }
    .relleno.disponible { background: #2e9d52; }
    .relleno.pocos { background: #e0a100; }
    .relleno.completo { background: #d93025; }
    .texto-cupos { font: var(--mat-sys-label-large); white-space: nowrap; }
    .texto-cupos.completo { color: var(--mat-sys-error); }
    .acciones { display: flex; justify-content: flex-end; margin-top: 8px; }
    .acciones button { min-width: 130px; }
    .girando { display: inline-block; margin-right: 8px; vertical-align: middle; }
    .completo { color: var(--mat-sys-error); }
  `,
})
export class ClaseCardComponent {
  readonly clase = input.required<Clase>();
  /** 'reservar' muestra el botón (clientes); 'lectura' solo la información. */
  readonly modo = input<'reservar' | 'lectura'>('lectura');
  readonly reservada = input(false);
  readonly procesando = input(false);
  readonly reservar = output<Clase>();

  protected readonly etiqueta = computed(() => etiquetaDisciplina(this.clase().disciplina));
  protected readonly icono = computed(() => iconoDisciplina(this.clase().disciplina));
  protected readonly inicio = computed(() => hora(this.clase().horaInicio));
  protected readonly fin = computed(() => hora(this.clase().horaFin));
  protected readonly finalizada = computed(() => yaInicio(this.clase().fecha, this.clase().horaInicio));
  protected readonly nivel = computed(() => nivelCupos(this.clase().cuposOcupados, this.clase().aforoMaximo));
  protected readonly porcentaje = computed(() =>
    Math.min(100, Math.round((this.clase().cuposOcupados / this.clase().aforoMaximo) * 100)),
  );
  protected readonly textoCupos = computed(() => `${this.clase().cuposOcupados}/${this.clase().aforoMaximo} cupos`);
}
