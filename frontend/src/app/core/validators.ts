import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { hoyIso } from './utils';

/** Mismas reglas que el backend (Bean Validation de los DTO). */
export const PATRON_DNI = /^\d{8}$/;
export const PATRON_TELEFONO = /^\d{9}$/;

/** La fecha (yyyy-MM-dd) no puede ser anterior a hoy (@FutureOrPresent). */
export const fechaNoPasada: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = control.value as string | null;
  return valor && valor < hoyIso() ? { fechaPasada: true } : null;
};

/** Validador de grupo: horaFin debe ser posterior a horaInicio (@AssertTrue del backend). */
export function horarioValido(inicio = 'horaInicio', fin = 'horaFin'): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const hi = grupo.get(inicio)?.value as string | null;
    const hf = grupo.get(fin)?.value as string | null;
    return hi && hf && hf <= hi ? { horarioInvalido: true } : null;
  };
}

/** Contraseña opcional: vacía o entre 8 y 100 caracteres (igual que SocioRequest). */
export const passwordOpcional: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const v = (control.value as string | null) ?? '';
  return v === '' || (v.length >= 8 && v.length <= 100) ? null : { passwordLongitud: true };
};
