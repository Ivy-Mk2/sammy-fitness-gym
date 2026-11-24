import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type Tema = 'claro' | 'oscuro';
export const CLAVE_TEMA = 'tema';

/** Modo claro/oscuro. La preferencia se guarda en localStorage ("tema"). */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly documento = inject(DOCUMENT);
  readonly tema = signal<Tema>(this.temaInicial());

  constructor() {
    this.aplicar(this.tema());
  }

  alternar(): void {
    this.establecer(this.tema() === 'oscuro' ? 'claro' : 'oscuro');
  }

  establecer(tema: Tema): void {
    this.tema.set(tema);
    this.aplicar(tema);
    try {
      localStorage.setItem(CLAVE_TEMA, tema);
    } catch {
      /* almacenamiento no disponible */
    }
  }

  private aplicar(tema: Tema): void {
    const html = this.documento.documentElement;
    html.classList.toggle('tema-oscuro', tema === 'oscuro');
    html.style.colorScheme = tema === 'oscuro' ? 'dark' : 'light';
  }

  private temaInicial(): Tema {
    try {
      const guardado = localStorage.getItem(CLAVE_TEMA);
      if (guardado === 'claro' || guardado === 'oscuro') {
        return guardado;
      }
    } catch {
      /* ignorar */
    }
    const prefiereOscuro = typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: dark)').matches;
    return prefiereOscuro ? 'oscuro' : 'claro';
  }
}
