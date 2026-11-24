package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.EstadoReserva;

import java.time.LocalDateTime;

public record InscritoResponse(
        Long reservaId,
        Long clienteId,
        String nombreCompleto,
        String dni,
        String telefono,
        String correo,
        EstadoReserva estado,
        LocalDateTime fechaReserva
) {
}
