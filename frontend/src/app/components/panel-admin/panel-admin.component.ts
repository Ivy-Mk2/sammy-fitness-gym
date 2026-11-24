import { Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ETIQUETA_ROL, Rol } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';

interface Subvista {
  ruta: string;
  texto: string;
  icono: string;
  roles: Rol[];
}

const SUBVISTAS: Subvista[] = [
  { ruta: 'clases', texto: 'Clases', icono: 'event_note', roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
  { ruta: 'socios', texto: 'Socios', icono: 'group', roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
  { ruta: 'inscritos', texto: 'Inscritos', icono: 'how_to_reg', roles: ['ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA'] },
];

/** Panel de gestión con subvistas según el rol: clases, socios e inscritos. */
@Component({
  selector: 'app-panel-admin',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabsModule, MatIconModule],
  template: `
    <header class="encabezado">
      <h1>Panel de gestión</h1>
      <p>{{ rol() }}</p>
    </header>

    <nav mat-tab-nav-bar [tabPanel]="panel" mat-stretch-tabs="false" aria-label="Secciones del panel">
      @for (s of subvistas(); track s.ruta) {
        <a mat-tab-link [routerLink]="s.ruta" routerLinkActive #rla="routerLinkActive" [active]="rla.isActive">
          <mat-icon class="icono">{{ s.icono }}</mat-icon>{{ s.texto }}
        </a>
      }
    </nav>
    <mat-tab-nav-panel #panel>
      <div class="subvista"><router-outlet /></div>
    </mat-tab-nav-panel>
  `,
  styles: `
    .encabezado h1 { font: var(--mat-sys-headline-small); margin: 0; }
    .encabezado p { margin: 2px 0 12px; color: var(--mat-sys-on-surface-variant); }
    .icono { margin-right: 6px; }
    .subvista { padding-top: 20px; }
  `,
})
export class PanelAdminComponent {
  private readonly auth = inject(AuthService);
  protected readonly subvistas = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? SUBVISTAS.filter((s) => s.roles.includes(rol)) : [];
  });
  protected readonly rol = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? `Sesión como ${ETIQUETA_ROL[rol].toLowerCase()}` : '';
  });
}
