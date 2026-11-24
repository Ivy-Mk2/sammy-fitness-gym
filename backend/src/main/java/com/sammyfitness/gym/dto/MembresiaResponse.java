package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.EstadoMembresia;

import java.math.BigDecimal;
import java.time.LocalDate;

/** @param diasRestantes días hasta el vencimiento (0 si ya venció). */
public record MembresiaResponse(
        Long id,
        Long socioId,
        Long planId,
        String planNombre,
        BigDecimal precio,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        EstadoMembresia estado,
        long diasRestantes
) {
}
