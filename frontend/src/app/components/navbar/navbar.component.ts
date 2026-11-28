import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ETIQUETA_ROL, Membresia, Rol } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { MembresiaService } from '../../core/services/membresia.service';
import { TemaService } from '../../core/services/tema.service';
import { CambiarPasswordDialogComponent } from './cambiar-password-dialog.component';

interface Enlace {
  ruta: string;
  texto: string;
  icono: string;
  roles: Rol[];
}

const ENLACES: Enlace[] = [
  { ruta: '/clases', texto: 'Clases', icono: 'event', roles: ['CLIENTE', 'ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA'] },
  { ruta: '/mis-reservas', texto: 'Mis reservas', icono: 'bookmark', roles: ['CLIENTE'] },
  { ruta: '/admin', texto: 'Panel', icono: 'dashboard', roles: ['ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA'] },
];

@Component({
  selector: 'app-navbar',
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    RouterLink,
    RouterLinkActive,
  ],
  template: `
    <mat-toolbar class="barra">
      <a routerLink="/" class="marca" aria-label="Sammy Fitness Gym, inicio">
        <mat-icon class="logo" aria-hidden="true">fitness_center</mat-icon>
        <span class="nombre">Sammy Fitness</span>
      </a>

      <nav class="enlaces" aria-label="Navegación principal">
        @for (e of enlaces(); track e.ruta) {
          <a mat-button [routerLink]="e.ruta" routerLinkActive="activo">
            <mat-icon>{{ e.icono }}</mat-icon>{{ e.texto }}
          </a>
        }
      </nav>

      <span class="espacio"></span>

      @if (membresia(); as m) {
        <span
          class="chip-membresia"
          [class.chip-ok]="m.estado === 'ACTIVA'"
          [class.chip-aviso]="m.estado === 'CONGELADA'"
          [class.chip-error]="m.estado === 'VENCIDA'"
          [matTooltip]="'Vence el ' + m.fechaVencimiento"
        >
          <mat-icon aria-hidden="true">card_membership</mat-icon>
          {{ m.planNombre }} · {{ etiquetaEstado(m) }}
        </span>
      }

      <button
        mat-icon-button
        (click)="tema.alternar()"
        [matTooltip]="tema.tema() === 'oscuro' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
        [attr.aria-label]="tema.tema() === 'oscuro' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
      >
        <mat-icon>{{ tema.tema() === 'oscuro' ? 'light_mode' : 'dark_mode' }}</mat-icon>
      </button>

      <button mat-button [matMenuTriggerFor]="menuUsuario" class="usuario" aria-label="Menú de usuario">
        <mat-icon>account_circle</mat-icon>
        <span class="usuario-texto">
          <span class="usuario-nombre">{{ auth.usuario()?.nombre }}</span>
          <span class="usuario-rol">{{ etiquetaRol() }}</span>
        </span>
      </button>
      <mat-menu #menuUsuario="matMenu" xPosition="before">
        <div class="menu-cabecera" mat-menu-item disabled>
          <mat-icon>badge</mat-icon>{{ auth.usuario()?.nombre }} · {{ etiquetaRol() }}
        </div>
        @for (e of enlaces(); track e.ruta) {
          <a mat-menu-item [routerLink]="e.ruta" class="solo-movil">
            <mat-icon>{{ e.icono }}</mat-icon>{{ e.texto }}
          </a>
        }
        <button mat-menu-item (click)="abrirCambiarPassword()">
          <mat-icon>lock_reset</mat-icon>Cambiar contraseña
        </button>
        <button mat-menu-item (click)="auth.logout()">
          <mat-icon>logout</mat-icon>Cerrar sesión
        </button>
      </mat-menu>
    </mat-toolbar>
  `,
  styles: `
    .barra {
      position: sticky;
      top: 0;
      z-index: 100;
      gap: 8px;
      background: var(--mat-sys-surface-container);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .marca {
      display: flex;
      align-items: center;
      gap: 6px;
      color: inherit;
      text-decoration: none;
      margin-right: 12px;
    }
    .logo { color: var(--mat-sys-primary); }
    .nombre { font: var(--mat-sys-title-large); font-weight: 600; }
    .enlaces { display: flex; gap: 4px; }
    .enlaces mat-icon { margin-right: 4px; }
    .activo { background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container); }
    .espacio { flex: 1; }
    .usuario-texto { display: inline-flex; flex-direction: column; align-items: flex-start; line-height: 1.1; margin-left: 6px; }
    .usuario-nombre { font: var(--mat-sys-label-large); }
    .usuario-rol { font: var(--mat-sys-label-small); color: var(--mat-sys-on-surface-variant); }
    .chip-membresia {
      display: inline-flex; align-items: center; gap: 4px; padding: 2px 12px 2px 8px; border-radius: 16px;
      font: var(--mat-sys-label-medium); background: var(--mat-sys-surface-container-high);
      border: 1px solid var(--mat-sys-outline-variant);
    }
    .chip-membresia mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .chip-ok { background: color-mix(in srgb, #2e7d32 18%, transparent); border-color: #2e7d32; }
    .chip-aviso { background: color-mix(in srgb, #ed6c02 18%, transparent); border-color: #ed6c02; }
    .chip-error { background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container); border-color: var(--mat-sys-error); }
    .solo-movil { display: none; }
    @media (max-width: 760px) {
      .enlaces, .usuario-texto, .nombre, .chip-membresia { display: none; }
      .solo-movil { display: flex; }
    }
  `,
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);
  protected readonly tema = inject(TemaService);
  private readonly dialog = inject(MatDialog);

  protected readonly enlaces = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? ENLACES.filter((e) => e.roles.includes(rol)) : [];
  });

  /** Membresía del cliente autenticado (se muestra en la barra; null para el personal o si no tiene). */
  protected readonly membresia = signal<Membresia | null>(null);

  private readonly membresias = inject(MembresiaService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const rol = this.auth.usuario()?.rol;
      if (rol === 'CLIENTE') {
        this.membresias
          .miMembresia()
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({ next: (m) => this.membresia.set(m), error: () => this.membresia.set(null) });
      } else {
        this.membresia.set(null);
      }
    });
  }

  protected abrirCambiarPassword(): void {
    this.dialog.open(CambiarPasswordDialogComponent, { width: '400px' });
  }

  protected etiquetaEstado(m: Membresia): string {
    return m.estado === 'ACTIVA' ? 'Activa' : m.estado === 'VENCIDA' ? 'Vencida' : 'Congelada';
  }

  protected readonly etiquetaRol = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? ETIQUETA_ROL[rol] : '';
  });
}