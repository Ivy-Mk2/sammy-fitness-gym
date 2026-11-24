import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ToastService } from '../../core/services/toast.service';

/** Muestra los toasts de éxito / error / información (esquina inferior derecha). */
@Component({
  selector: 'app-toast',
  imports: [MatIconModule],
  template: `
    <div class="contenedor" aria-live="polite" aria-atomic="false">
      @for (t of toasts.toasts(); track t.id) {
        <div class="toast" [class]="'toast ' + t.tipo" role="status">
          <mat-icon aria-hidden="true">{{
            t.tipo === 'exito' ? 'check_circle' : t.tipo === 'error' ? 'error' : 'info'
          }}</mat-icon>
          <span class="mensaje">{{ t.mensaje }}</span>
          <button type="button" class="cerrar" (click)="toasts.cerrar(t.id)" aria-label="Cerrar notificación">
            <mat-icon>close</mat-icon>
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .contenedor {
      position: fixed;
      right: 16px;
      bottom: 16px;
      left: 16px;
      z-index: 2000;
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 8px;
      pointer-events: none;
    }
    .toast {
      pointer-events: auto;
      display: flex;
      align-items: center;
      gap: 10px;
      width: min(420px, 100%);
      box-sizing: border-box;
      padding: 12px 8px 12px 14px;
      border-radius: 12px;
      box-shadow: 0 6px 20px rgb(0 0 0 / 0.25);
      color: #fff;
      font: var(--mat-sys-body-medium);
      animation: entrar 0.2s ease-out;
    }
    .exito { background: #1b7f3b; }
    .error { background: #b3261e; }
    .info { background: #3a4a63; }
    .mensaje { flex: 1; }
    .cerrar {
      border: 0;
      background: transparent;
      color: inherit;
      cursor: pointer;
      display: flex;
      padding: 4px;
      border-radius: 50%;
    }
    .cerrar:focus-visible { outline: 2px solid #fff; }
    @keyframes entrar {
      from { transform: translateY(12px); opacity: 0; }
      to { transform: none; opacity: 1; }
    }
    @media (prefers-reduced-motion: reduce) {
      .toast { animation: none; }
    }
  `,
})
export class ToastComponent {
  protected readonly toasts = inject(ToastService);
}
