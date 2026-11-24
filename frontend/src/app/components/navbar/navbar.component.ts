import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ETIQUETA_ROL, Rol } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { TemaService } from '../../core/services/tema.service';

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
  imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule, RouterLink, RouterLinkActive],
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
    .solo-movil { display: none; }
    @media (max-width: 760px) {
      .enlaces, .usuario-texto, .nombre { display: none; }
      .solo-movil { display: flex; }
    }
  `,
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);
  protected readonly tema = inject(TemaService);

  protected readonly enlaces = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? ENLACES.filter((e) => e.roles.includes(rol)) : [];
  });

  protected readonly etiquetaRol = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? ETIQUETA_ROL[rol] : '';
  });
}
