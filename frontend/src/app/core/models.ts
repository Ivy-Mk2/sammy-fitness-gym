/** Tipos que reflejan los DTO del backend. */

export type Rol = 'ADMINISTRADOR' | 'RECEPCIONISTA' | 'ENTRENADOR' | 'CLIENTE';
export type Disciplina = 'SPINNING' | 'CROSSFIT' | 'YOGA' | 'FUNCIONAL' | 'ZUMBA' | 'HALTEROFILIA';
export type Horario = 'MANANA' | 'TARDE' | 'NOCHE';
export type EstadoReserva = 'CONFIRMADA' | 'ASISTIDA' | 'CANCELADA';
export type EstadoMembresia = 'ACTIVA' | 'VENCIDA' | 'CONGELADA';

export const DISCIPLINAS: { valor: Disciplina; etiqueta: string; icono: string }[] = [
  { valor: 'SPINNING', etiqueta: 'Spinning', icono: 'directions_bike' },
  { valor: 'CROSSFIT', etiqueta: 'Crossfit', icono: 'fitness_center' },
  { valor: 'YOGA', etiqueta: 'Yoga', icono: 'self_improvement' },
  { valor: 'FUNCIONAL', etiqueta: 'Funcional', icono: 'sports_gymnastics' },
  { valor: 'ZUMBA', etiqueta: 'Zumba', icono: 'music_note' },
  { valor: 'HALTEROFILIA', etiqueta: 'Halterofilia', icono: 'sports_martial_arts' },
];

export const HORARIOS: { valor: Horario; etiqueta: string }[] = [
  { valor: 'MANANA', etiqueta: 'Mañana (antes de 12:00)' },
  { valor: 'TARDE', etiqueta: 'Tarde (12:00 – 17:59)' },
  { valor: 'NOCHE', etiqueta: 'Noche (desde 18:00)' },
];

export const ETIQUETA_ROL: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  RECEPCIONISTA: 'Recepcionista',
  ENTRENADOR: 'Entrenador',
  CLIENTE: 'Cliente',
};

export function etiquetaDisciplina(d: Disciplina): string {
  return DISCIPLINAS.find((x) => x.valor === d)?.etiqueta ?? d;
}

export function iconoDisciplina(d: Disciplina): string {
  return DISCIPLINAS.find((x) => x.valor === d)?.icono ?? 'sports';
}

/** Usuario de la sesión, obtenido del payload del JWT. */
export interface UsuarioSesion {
  id: number;
  correo: string;
  nombre: string;
  rol: Rol;
  /** Expiración del token (epoch en ms). */
  expira: number;
}

export interface LoginResponse {
  token: string;
  rol: Rol;
  nombre: string;
}

export interface Clase {
  id: number;
  nombre: string;
  disciplina: Disciplina;
  entrenadorId: number;
  entrenadorNombre: string;
  fecha: string; // yyyy-MM-dd
  horaInicio: string; // HH:mm[:ss]
  horaFin: string;
  aforoMaximo: number;
  cuposOcupados: number;
  cuposDisponibles: number;
  descripcion: string | null;
}

export interface ClaseRequest {
  nombre: string;
  disciplina: Disciplina;
  entrenadorId: number | null;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  aforoMaximo: number;
  descripcion: string | null;
}

export interface FiltrosClase {
  disciplina?: Disciplina | null;
  dia?: string | null;
  horario?: Horario | null;
  entrenadorId?: number | null;
}

export interface Entrenador {
  id: number;
  nombre: string;
}

export interface Reserva {
  id: number;
  claseId: number;
  claseNombre: string;
  disciplina: Disciplina;
  entrenadorNombre: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  estado: EstadoReserva;
  fechaReserva: string;
  cancelable: boolean;
}

export interface Inscrito {
  reservaId: number;
  clienteId: number;
  nombreCompleto: string;
  dni: string;
  telefono: string;
  correo: string;
  estado: EstadoReserva;
  fechaReserva: string;
}

export interface Plan {
  id: number;
  nombre: string;
  duracionDias: number;
  precio: number;
}

export interface Membresia {
  id: number;
  socioId: number;
  planId: number;
  planNombre: string;
  precio: number;
  fechaInicio: string;
  fechaVencimiento: string;
  estado: EstadoMembresia;
  diasRestantes: number;
}

export interface Socio {
  id: number;
  nombres: string;
  apellidos: string;
  dni: string;
  correo: string;
  telefono: string;
  activo: boolean;
  membresiaActual: Membresia | null;
}

export interface SocioRequest {
  nombres: string;
  apellidos: string;
  dni: string;
  correo: string;
  telefono: string;
  password: string | null;
  activo: boolean | null;
}

/** Cuerpo uniforme de error del backend. */
export interface ErrorApi {
  fecha: string;
  codigo: number;
  mensaje: string;
  ruta: string;
}
