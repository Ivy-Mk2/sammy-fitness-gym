import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { ToastComponent } from './components/toast/toast.component';
import { AuthService } from './core/services/auth.service';
import { TemaService } from './core/services/tema.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, ToastComponent],
  template: `
    @if (auth.usuario()) {
      <app-navbar />
    }
    <main class="contenido" [class.sin-navbar]="!auth.usuario()">
      <router-outlet />
    </main>
    <app-toast />
  `,
  styles: `
    .contenido {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px 16px 48px;
      box-sizing: border-box;
    }
    .sin-navbar {
      max-width: none;
      padding: 0;
    }
  `,
})
export class App {
  protected readonly auth = inject(AuthService);
  // Instanciar el servicio aplica el tema guardado al arrancar.
  private readonly tema = inject(TemaService);
}
