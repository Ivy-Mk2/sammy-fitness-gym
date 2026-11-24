import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { EstadoMembresia, Plan, Socio, SocioRequest } from '../../core/models';
import { MembresiaService } from '../../core/services/membresia.service';
import { ToastService } from '../../core/services/toast.service';
import { hoyIso, mensajeError } from '../../core/utils';
import { PATRON_DNI, PATRON_TELEFONO, passwordOpcional } from '../../core/validators';
import { confirmar } from '../confirm-dialog/confirm-dialog.component';

type Panel = { tipo: 'socio'; socio: Socio | null } | { tipo: 'membresia'; socio: Socio } | null;

/** Subvista "Socios": registrar, editar, asignar membresía y cambiar su estado (ADMINISTRADOR y RECEPCIONISTA). */
@Component({
  selector: 'app-admin-socios',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="barra-acciones">
      <h2>Socios</h2>
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="buscador">
        <mat-label>Buscar por nombre, DNI o correo</mat-label>
        <input matInput [formControl]="busqueda" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>
      @if (!panel()) {
        <button mat-flat-button (click)="nuevoSocio()"><mat-icon>person_add</mat-icon>Registrar socio</button>
      }
    </div>

    <!-- Formulario de socio -->
    @if (panel()?.tipo === 'socio') {
      <mat-card appearance="outlined" class="formulario">
        <h3>{{ panel()?.socio ? 'Editar socio' : 'Registrar socio' }}</h3>
        <form [formGroup]="formSocio" (ngSubmit)="guardarSocio()" novalidate>
          <div class="campos">
            <mat-form-field appearance="outline">
              <mat-label>Nombres</mat-label>
              <input matInput formControlName="nombres" maxlength="80" />
              <mat-error>Los nombres son obligatorios (máx. 80)</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Apellidos</mat-label>
              <input matInput formControlName="apellidos" maxlength="80" />
              <mat-error>Los apellidos son obligatorios (máx. 80)</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>DNI</mat-label>
              <input matInput formControlName="dni" inputmode="numeric" maxlength="8" />
              <mat-error>El DNI debe tener exactamente 8 dígitos</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Teléfono</mat-label>
              <input matInput formControlName="telefono" inputmode="numeric" maxlength="9" />
              <mat-error>El teléfono debe tener exactamente 9 dígitos</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Correo</mat-label>
              <input matInput type="email" formControlName="correo" maxlength="120" />
              <mat-error>Ingrese un correo válido</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ panel()?.socio ? 'Nueva contraseña (opcional)' : 'Contraseña' }}</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="new-password" />
              <mat-hint>Entre 8 y 100 caracteres</mat-hint>
              <mat-error>La contraseña debe tener entre 8 y 100 caracteres</mat-error>
            </mat-form-field>
            @if (panel()?.socio) {
              <mat-slide-toggle formControlName="activo" class="ancho-completo">Acceso activo</mat-slide-toggle>
            }
          </div>
          <div class="botones-form">
            <button mat-button type="button" (click)="panel.set(null)">Cancelar</button>
            <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
          </div>
        </form>
      </mat-card>
    }

    <!-- Formulario de membresía -->
    @if (panel()?.tipo === 'membresia') {
      <mat-card appearance="outlined" class="formulario">
        <h3>Asignar membresía a {{ panel()?.socio?.nombres }} {{ panel()?.socio?.apellidos }}</h3>
        <form [formGroup]="formMembresia" (ngSubmit)="guardarMembresia()" novalidate>
          <div class="campos">
            <mat-form-field appearance="outline">
              <mat-label>Plan</mat-label>
              <mat-select formControlName="planId">
                @for (p of planes(); track p.id) {
                  <mat-option [value]="p.id">{{ p.nombre }} · {{ p.duracionDias }} días · S/ {{ p.precio }}</mat-option>
                }
              </mat-select>
              <mat-error>Seleccione un plan</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Fecha de inicio</mat-label>
              <input matInput type="date" formControlName="fechaInicio" />
              <mat-hint>El vencimiento se calcula según la duración del plan</mat-hint>
            </mat-form-field>
          </div>
          <div class="botones-form">
            <button mat-button type="button" (click)="panel.set(null)">Cancelar</button>
            <button mat-flat-button type="submit" [disabled]="guardando()">Asignar</button>
          </div>
        </form>
      </mat-card>
    }

    @if (cargando()) {
      <div class="cargando"><mat-spinner diameter="40" /></div>
    } @else if (socios().length === 0) {
      <p class="estado-vacio">No se encontraron socios.</p>
    } @else {
      <div class="tabla-scroll">
        <table mat-table [dataSource]="socios()">
          <ng-container matColumnDef="socio">
            <th mat-header-cell *matHeaderCellDef>Socio</th>
            <td mat-cell *matCellDef="let s">
              {{ s.apellidos }}, {{ s.nombres }}
              @if (!s.activo) { <span class="chip INACTIVO">Inactivo</span> }
              <br /><span class="secundario">DNI {{ s.dni }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="contacto">
            <th mat-header-cell *matHeaderCellDef>Contacto</th>
            <td mat-cell *matCellDef="let s">{{ s.correo }}<br /><span class="secundario">{{ s.telefono }}</span></td>
          </ng-container>
          <ng-container matColumnDef="membresia">
            <th mat-header-cell *matHeaderCellDef>Membresía</th>
            <td mat-cell *matCellDef="let s">
              @if (s.membresiaActual; as m) {
                <span class="chip" [class]="'chip ' + m.estado">{{ m.estado }}</span>
                <br /><span class="secundario">{{ m.planNombre }} · vence {{ m.fechaVencimiento | date: 'd MMM y' }}</span>
              } @else {
                <span class="secundario">Sin membresía</span>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef><span class="cdk-visually-hidden">Acciones</span></th>
            <td mat-cell *matCellDef="let s" class="acciones-celda">
              <button mat-icon-button (click)="editarSocio(s)" matTooltip="Editar" aria-label="Editar socio">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button (click)="nuevaMembresia(s)" matTooltip="Asignar membresía" aria-label="Asignar membresía">
                <mat-icon>card_membership</mat-icon>
              </button>
              @if (s.membresiaActual) {
                <button mat-icon-button [matMenuTriggerFor]="menuEstado" matTooltip="Cambiar estado de la membresía"
                        aria-label="Cambiar estado de la membresía">
                  <mat-icon>more_vert</mat-icon>
                </button>
                <mat-menu #menuEstado="matMenu">
                  @for (e of estados; track e) {
                    <button mat-menu-item [disabled]="e === s.membresiaActual.estado" (click)="cambiarEstado(s, e)">
                      Marcar {{ e }}
                    </button>
                  }
                </mat-menu>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas"></tr>
        </table>
      </div>
    }
  `,
  styleUrl: './admin.scss',
  styles: `
    .buscador { flex: 1; min-width: 220px; max-width: 420px; }
  `,
})
export class AdminSociosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(MembresiaService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columnas = ['socio', 'contacto', 'membresia', 'acciones'];
  protected readonly estados: EstadoMembresia[] = ['ACTIVA', 'CONGELADA', 'VENCIDA'];

  protected readonly socios = signal<Socio[]>([]);
  protected readonly planes = signal<Plan[]>([]);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly panel = signal<Panel>(null);

  protected readonly busqueda = new FormControl('', { nonNullable: true });

  // Mismas validaciones que SocioRequest del backend
  protected readonly formSocio = this.fb.nonNullable.group({
    nombres: ['', [Validators.required, Validators.maxLength(80)]],
    apellidos: ['', [Validators.required, Validators.maxLength(80)]],
    dni: ['', [Validators.required, Validators.pattern(PATRON_DNI)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    telefono: ['', [Validators.required, Validators.pattern(PATRON_TELEFONO)]],
    password: [''],
    activo: [true],
  });

  protected readonly formMembresia = this.fb.group({
    planId: this.fb.control<number | null>(null, Validators.required),
    fechaInicio: this.fb.control<string>(hoyIso()),
  });

  ngOnInit(): void {
    this.cargar();
    this.service.planes().subscribe({ next: (p) => this.planes.set(p) });
    this.busqueda.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cargar());
  }

  protected nuevoSocio(): void {
    this.formSocio.reset({ nombres: '', apellidos: '', dni: '', correo: '', telefono: '', password: '', activo: true });
    this.formSocio.controls.password.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(100)]);
    this.formSocio.controls.password.updateValueAndValidity();
    this.panel.set({ tipo: 'socio', socio: null });
  }

  protected editarSocio(s: Socio): void {
    this.formSocio.reset({ ...s, password: '' });
    this.formSocio.controls.password.setValidators(passwordOpcional);
    this.formSocio.controls.password.updateValueAndValidity();
    this.panel.set({ tipo: 'socio', socio: s });
  }

  protected nuevaMembresia(s: Socio): void {
    this.formMembresia.reset({ planId: null, fechaInicio: hoyIso() });
    this.panel.set({ tipo: 'membresia', socio: s });
  }

  protected guardarSocio(): void {
    if (this.formSocio.invalid) {
      this.formSocio.markAllAsTouched();
      return;
    }
    const p = this.panel();
    const socio = p?.tipo === 'socio' ? p.socio : null;
    const v = this.formSocio.getRawValue();
    const datos: SocioRequest = {
      nombres: v.nombres.trim(),
      apellidos: v.apellidos.trim(),
      dni: v.dni,
      correo: v.correo.trim(),
      telefono: v.telefono,
      password: v.password || null,
      activo: socio ? v.activo : null,
    };
    this.guardando.set(true);
    (socio ? this.service.actualizarSocio(socio.id, datos) : this.service.crearSocio(datos)).subscribe({
      next: (s) => {
        this.guardando.set(false);
        this.toast.exito(socio ? 'Socio actualizado' : `Socio ${s.nombres} registrado. Ahora puede asignarle una membresía.`);
        this.panel.set(null);
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected guardarMembresia(): void {
    const p = this.panel();
    if (p?.tipo !== 'membresia') return;
    if (this.formMembresia.invalid) {
      this.formMembresia.markAllAsTouched();
      return;
    }
    const v = this.formMembresia.getRawValue();
    this.guardando.set(true);
    this.service.asignarMembresia(p.socio.id, v.planId!, v.fechaInicio || null).subscribe({
      next: (m) => {
        this.guardando.set(false);
        this.toast.exito(`Membresía ${m.planNombre} asignada (vence el ${m.fechaVencimiento})`);
        this.panel.set(null);
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected cambiarEstado(s: Socio, estado: EstadoMembresia): void {
    const m = s.membresiaActual;
    if (!m) return;
    confirmar(this.dialog, {
      titulo: 'Cambiar estado de membresía',
      mensaje: `¿Marcar la membresía ${m.planNombre} de ${s.nombres} ${s.apellidos} como ${estado}?`,
      confirmar: 'Cambiar',
      peligro: estado !== 'ACTIVA',
    }).subscribe((ok) => {
      if (!ok) return;
      this.service.cambiarEstado(m.id, estado).subscribe({
        next: () => {
          this.toast.exito(`Membresía marcada como ${estado}`);
          this.cargar();
        },
        error: (e) => this.toast.error(mensajeError(e)),
      });
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.service.socios(this.busqueda.value.trim() || undefined).subscribe({
      next: (s) => {
        this.socios.set(s);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }
}
