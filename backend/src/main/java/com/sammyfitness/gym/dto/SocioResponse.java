package com.sammyfitness.gym.dto;

/** @param membresiaActual la membresía vigente o, si no hay, la más reciente (null si nunca tuvo). */
public record SocioResponse(
        Long id,
        String nombres,
        String apellidos,
        String dni,
        String correo,
        String telefono,
        boolean activo,
        MembresiaResponse membresiaActual
) {
}
