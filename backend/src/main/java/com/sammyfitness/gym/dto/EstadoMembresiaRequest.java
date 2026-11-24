package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.EstadoMembresia;
import jakarta.validation.constraints.NotNull;

public record EstadoMembresiaRequest(
        @NotNull(message = "El estado es obligatorio (ACTIVA, VENCIDA o CONGELADA)")
        EstadoMembresia estado
) {
}
