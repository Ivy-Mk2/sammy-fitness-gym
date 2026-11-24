import { Injectable, signal } from '@angular/core';

export type TipoToast = 'exito' | 'error' | 'info';

export interface Toast {
  id: number;
  tipo: TipoToast;
  mensaje: string;
}

/** Notificaciones breves (toasts) mostradas por ToastComponent. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private siguienteId = 1;
  private readonly lista = signal<Toast[]>([]);
  readonly toasts = this.lista.asReadonly();

  exito(mensaje: string): void {
    this.mostrar('exito', mensaje, 4000);
  }

  error(mensaje: string): void {
    this.mostrar('error', mensaje, 6000);
  }

  info(mensaje: string): void {
    this.mostrar('info', mensaje, 4000);
  }

  cerrar(id: number): void {
    this.lista.update((l) => l.filter((t) => t.id !== id));
  }

  private mostrar(tipo: TipoToast, mensaje: string, duracionMs: number): void {
    const toast: Toast = { id: this.siguienteId++, tipo, mensaje };
    // máximo 4 visibles a la vez
    this.lista.update((l) => [...l.slice(-3), toast]);
    setTimeout(() => this.cerrar(toast.id), duracionMs);
  }
}
