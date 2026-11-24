import { FormControl, FormGroup, Validators } from '@angular/forms';
import { hoyIso } from './utils';
import { PATRON_DNI, PATRON_TELEFONO, fechaNoPasada, horarioValido, passwordOpcional } from './validators';

describe('Validadores (mismas reglas que el backend)', () => {
  it('fechaNoPasada rechaza ayer y acepta hoy', () => {
    const ayer = new Date();
    ayer.setDate(ayer.getDate() - 1);
    const ayerIso = `${ayer.getFullYear()}-${String(ayer.getMonth() + 1).padStart(2, '0')}-${String(ayer.getDate()).padStart(2, '0')}`;
    expect(fechaNoPasada(new FormControl(ayerIso))).toEqual({ fechaPasada: true });
    expect(fechaNoPasada(new FormControl(hoyIso()))).toBeNull();
  });

  it('horarioValido exige horaFin posterior a horaInicio', () => {
    const grupo = (hi: string, hf: string) =>
      new FormGroup({ horaInicio: new FormControl(hi), horaFin: new FormControl(hf) }, { validators: horarioValido() });
    expect(grupo('10:00', '09:00').errors).toEqual({ horarioInvalido: true });
    expect(grupo('10:00', '10:00').errors).toEqual({ horarioInvalido: true });
    expect(grupo('10:00', '11:00').errors).toBeNull();
  });

  it('DNI de 8 dígitos y teléfono de 9 dígitos', () => {
    expect(Validators.pattern(PATRON_DNI)(new FormControl('1234567A'))).not.toBeNull();
    expect(Validators.pattern(PATRON_DNI)(new FormControl('12345678'))).toBeNull();
    expect(Validators.pattern(PATRON_TELEFONO)(new FormControl('98765432'))).not.toBeNull();
    expect(Validators.pattern(PATRON_TELEFONO)(new FormControl('987654321'))).toBeNull();
  });

  it('contraseña opcional: vacía o entre 8 y 100 caracteres', () => {
    expect(passwordOpcional(new FormControl(''))).toBeNull();
    expect(passwordOpcional(new FormControl('corta'))).toEqual({ passwordLongitud: true });
    expect(passwordOpcional(new FormControl('suficiente'))).toBeNull();
  });
});
