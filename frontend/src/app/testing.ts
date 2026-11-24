import { Rol } from './core/models';

/** Crea un JWT falso (firma irrelevante: el frontend solo lee el payload). */
export function crearToken(opciones: { rol?: Rol; nombre?: string; expiraEnSeg?: number; id?: number } = {}): string {
  const { rol = 'CLIENTE', nombre = 'Ana Pérez', expiraEnSeg = 3600, id = 7 } = opciones;
  const b64 = (obj: object) => {
    const bytes = new TextEncoder().encode(JSON.stringify(obj));
    let binario = '';
    bytes.forEach((b) => (binario += String.fromCharCode(b)));
    return btoa(binario).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  };
  const ahora = Math.floor(Date.now() / 1000);
  return [
    b64({ alg: 'HS256' }),
    b64({ sub: 'ana@correo.test', id, rol, nombre, iat: ahora, exp: ahora + expiraEnSeg }),
    'firma',
  ].join('.');
}
