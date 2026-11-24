import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Clase, ClaseRequest, DISCIPLINAS, Disciplina, Entrenador, etiquetaDisciplina } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { ClaseService } from '../../core/services/clase.service';
import { ToastService } from '../../core/services/toast.service';
import { hora, hoyIso, mensajeError } from '../../core/utils';
import { fechaNoPasada, horarioValido } from '../../core/validators';
import { confirmar } from '../confirm-dialog/confirm-dialog.component';

/** Subvista "Clases" del panel: crear, editar y eliminar (ADMINISTRADOR y ENTRENADOR). */
@Component({
  selector: 'app-admin-clases',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="barra-acciones">
      <h2>{{ esEntrenador() ? 'Mis clases' : 'Clases programadas' }}</h2>
      @if (!formVisible()) {
        <button mat-flat-button (click)="nueva()"><mat-icon>add</mat-icon>Nueva clase</button>
      }
    </div>

    @if (formVisible()) {
      <mat-card appearance="outlined" class="formulario">
        <h3>{{ editando() ? 'Editar clase' : 'Nueva clase' }}</h3>
        <form [formGroup]="form" (ngSubmit)="guardar()" novalidate>
          <div class="campos">
            <mat-form-field appearance="outline">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" maxlength="60" />
              <mat-hint align="end">{{ form.controls.nombre.value.length }}/60</mat-hint>
              @if (form.controls.nombre.hasError('required')) {
                <mat-error>El nombre es obligatorio</mat-error>
              } @else if (form.controls.nombre.hasError('minlength')) {
                <mat-error>Debe tener entre 3 y 60 caracteres</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Disciplina</mat-label>
              <mat-select formControlName="disciplina">
                @for (d of disciplinas; track d.valor) {
                  <mat-option [value]="d.valor">{{ d.etiqueta }}</mat-option>
                }
              </mat-select>
              <mat-error>La disciplina es obligatoria</mat-error>
            </mat-form-field>

            @if (!esEntrenador()) {
              <mat-form-field appearance="outline">
                <mat-label>Entrenador</mat-label>
                <mat-select formControlName="entrenadorId">
                  @for (e of entrenadores(); track e.id) {
                    <mat-option [value]="e.id">{{ e.nombre }}</mat-option>
                  }
                </mat-select>
                <mat-error>El entrenador es obligatorio</mat-error>
              </mat-form-field>
            }

            <mat-form-field appearance="outline">
              <mat-label>Fecha</mat-label>
              <input matInput type="date" formControlName="fecha" [min]="hoy" />
              @if (form.controls.fecha.hasError('required')) {
                <mat-error>La fecha es obligatoria</mat-error>
              } @else if (form.controls.fecha.hasError('fechaPasada')) {
                <mat-error>La fecha no puede ser pasada</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Hora de inicio</mat-label>
              <input matInput type="time" formControlName="horaInicio" />
              <mat-error>La hora de inicio es obligatoria</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Hora de fin</mat-label>
              <input matInput type="time" formControlName="horaFin" />
              <mat-error>La hora de fin es obligatoria</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Aforo máximo</mat-label>
              <input matInput type="number" formControlName="aforoMaximo" min="1" max="50" />
              <mat-error>El aforo debe estar entre 1 y 50</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="ancho-completo">
              <mat-label>Descripción (opcional)</mat-label>
              <textarea matInput formControlName="descripcion" rows="2" maxlength="200"></textarea>
              <mat-hint align="end">{{ form.controls.descripcion.value.length }}/200</mat-hint>
              <mat-error>Máximo 200 caracteres</mat-error>
            </mat-form-field>
          </div>

          @if (form.hasError('horarioInvalido') && form.controls.horaFin.touched) {
            <p class="error-grupo" role="alert">La hora de fin debe ser posterior a la hora de inicio</p>
          }

          <div class="botones-form">
            <button mat-button type="button" (click)="cerrarForm()">Cancelar</button>
            <button mat-flat-button type="submit" [disabled]="guardando()">
              {{ editando() ? 'Guardar cambios' : 'Crear clase' }}
            </button>
          </div>
        </form>
      </mat-card>
    }

    @if (cargando()) {
      <div class="cargando"><mat-spinner diameter="40" /></div>
    } @else if (clases().length === 0) {
      <p class="estado-vacio">No hay clases programadas desde hoy.</p>
    } @else {
      <div class="tabla-scroll">
        <table mat-table [dataSource]="clases()">
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let c">
              {{ c.fecha | date: 'EEE d MMM' }}<br /><span class="secundario">{{ h(c.horaInicio) }}–{{ h(c.horaFin) }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="clase">
            <th mat-header-cell *matHeaderCellDef>Clase</th>
            <td mat-cell *matCellDef="let c">
              {{ c.nombre }}<br /><span class="secundario">{{ disciplina(c.disciplina) }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="entrenador">
            <th mat-header-cell *matHeaderCellDef>Entrenador</th>
            <td mat-cell *matCellDef="let c">{{ c.entrenadorNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="cupos">
            <th mat-header-cell *matHeaderCellDef>Cupos</th>
            <td mat-cell *matCellDef="let c">{{ c.cuposOcupados }}/{{ c.aforoMaximo }}</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef><span class="cdk-visually-hidden">Acciones</span></th>
            <td mat-cell *matCellDef="let c" class="acciones-celda">
              <a mat-icon-button routerLink="../inscritos" [queryParams]="{ claseId: c.id }" matTooltip="Ver inscritos"
                 aria-label="Ver inscritos"><mat-icon>group</mat-icon></a>
              <button mat-icon-button (click)="editar(c)" matTooltip="Editar" aria-label="Editar clase">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button class="peligro" (click)="eliminar(c)" matTooltip="Eliminar" aria-label="Eliminar clase">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
        </table>
      </div>
    }
  `,
  styleUrl: './admin.scss',
})
export class AdminClasesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly claseService = inject(ClaseService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  protected readonly disciplinas = DISCIPLINAS;
  protected readonly columnas = ['fecha', 'clase', 'entrenador', 'cupos', 'acciones'];
  protected readonly hoy = hoyIso();
  protected readonly h = hora;
  protected readonly disciplina = etiquetaDisciplina;

  protected readonly clases = signal<Clase[]>([]);
  protected readonly entrenadores = signal<Entrenador[]>([]);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly formVisible = signal(false);
  protected readonly editando = signal<Clase | null>(null);
  protected readonly esEntrenador = computed(() => this.auth.usuario()?.rol === 'ENTRENADOR');

  // Mismas validaciones que ClaseRequest del backend
  protected readonly form = this.fb.nonNullable.group(
    {
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(60)]],
      disciplina: this.fb.nonNullable.control<Disciplina | ''>('', Validators.required),
      entrenadorId: this.fb.control<number | null>(null),
      fecha: ['', [Validators.required, fechaNoPasada]],
      horaInicio: ['', Validators.required],
      horaFin: ['', Validators.required],
      aforoMaximo: [20, [Validators.required, Validators.min(1), Validators.max(50)]],
      descripcion: ['', Validators.maxLength(200)],
    },
    { validators: horarioValido() },
  );

  ngOnInit(): void {
    if (!this.esEntrenador()) {
      this.form.controls.entrenadorId.addValidators(Validators.required);
      this.claseService.entrenadores().subscribe({ next: (e) => this.entrenadores.set(e) });
    }
    this.cargar();
  }

  protected nueva(): void {
    this.editando.set(null);
    this.form.reset({ nombre: '', disciplina: '', entrenadorId: null, fecha: '', horaInicio: '', horaFin: '', aforoMaximo: 20, descripcion: '' });
    this.formVisible.set(true);
  }

  protected editar(c: Clase): void {
    this.editando.set(c);
    this.form.reset({
      nombre: c.nombre,
      disciplina: c.disciplina,
      entrenadorId: c.entrenadorId,
      fecha: c.fecha,
      horaInicio: hora(c.horaInicio),
      horaFin: hora(c.horaFin),
      aforoMaximo: c.aforoMaximo,
      descripcion: c.descripcion ?? '',
    });
    this.formVisible.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cerrarForm(): void {
    this.formVisible.set(false);
    this.editando.set(null);
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const datos: ClaseRequest = {
      nombre: v.nombre.trim(),
      disciplina: v.disciplina as Disciplina,
      entrenadorId: this.esEntrenador() ? null : v.entrenadorId,
      fecha: v.fecha,
      horaInicio: v.horaInicio,
      horaFin: v.horaFin,
      aforoMaximo: Number(v.aforoMaximo),
      descripcion: v.descripcion.trim() || null,
    };
    const actual = this.editando();
    const peticion = actual ? this.claseService.actualizar(actual.id, datos) : this.claseService.crear(datos);
    this.guardando.set(true);
    peticion.subscribe({
      next: (c) => {
        this.guardando.set(false);
        this.toast.exito(actual ? `Clase "${c.nombre}" actualizada` : `Clase "${c.nombre}" creada`);
        this.cerrarForm();
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected eliminar(c: Clase): void {
    confirmar(this.dialog, {
      titulo: 'Eliminar clase',
      mensaje: `¿Eliminar "${c.nombre}" del ${c.fecha}? Esta acción no se puede deshacer.`,
      confirmar: 'Eliminar',
      peligro: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.claseService.eliminar(c.id).subscribe({
        next: () => {
          this.toast.exito('Clase eliminada');
          this.cargar();
        },
        error: (e) => this.toast.error(mensajeError(e)),
      });
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    const filtro = this.esEntrenador() ? { entrenadorId: this.auth.usuarioActual?.id } : {};
    this.claseService.listar(filtro).subscribe({
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
}
