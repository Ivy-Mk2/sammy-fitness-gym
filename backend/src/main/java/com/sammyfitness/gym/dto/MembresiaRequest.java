package com.sammyfitness.gym.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** fechaInicio es opcional: si no se envía, la membresía empieza hoy. */
public record MembresiaRequest(
        @NotNull(message = "El plan es obligatorio")
        @Positive(message = "El id del plan no es válido")
        Long planId,

        LocalDate fechaInicio
) {
}
