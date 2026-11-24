package com.sammyfitness.gym.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservaRequest(
        @NotNull(message = "La clase es obligatoria")
        @Positive(message = "El id de la clase no es válido")
        Long claseId
) {
}
