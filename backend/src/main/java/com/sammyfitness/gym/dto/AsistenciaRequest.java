package com.sammyfitness.gym.dto;

import jakarta.validation.constraints.NotNull;

/** asistio=true marca ASISTIDA; false la devuelve a CONFIRMADA (corrección). */
public record AsistenciaRequest(
        @NotNull(message = "Indique si el cliente asistió")
        Boolean asistio
) {
}
