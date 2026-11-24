import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from './models';

/** Extrae un mensaje legible de un error HTTP del backend ({ fecha, codigo, mensaje, ruta }). */
export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor. Verifique que el backend esté encendido.';
    }
    const cuerpo = error.error as Partial<ErrorApi> | null;
    if (cuerpo && typeof cuerpo.mensaje === 'string' && cuerpo.mensaje) {
      return cuerpo.mensaje;
    }
    return `Error ${error.status}: ${error.statusText || 'error inesperado'}`;
  }
  return 'Ocurrió un error inesperado';
}

/** "07:00:00" → "07:00". */
export function hora(valor: string): string {
  return valor ? valor.substring(0, 5) : '';
}

/** Fecha local de hoy en formato yyyy-MM-dd (sin desfase UTC). */
export function hoyIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** true si la clase (fecha + hora de inicio) ya empezó según la hora local. */
export function yaInicio(fecha: string, horaInicio: string): boolean {
  return new Date(`${fecha}T${hora(horaInicio)}:00`).getTime() <= Date.now();
}

function pad(n: number): string {
  return String(n).padStart(2, '0');
}
