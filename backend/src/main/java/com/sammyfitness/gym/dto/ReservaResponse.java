package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.Disciplina;
import com.sammyfitness.gym.model.EstadoReserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @param cancelable true si la reserva está CONFIRMADA y la clase aún no empieza.
 */
public record ReservaResponse(
        Long id,
        Long claseId,
        String claseNombre,
        Disciplina disciplina,
        String entrenadorNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        EstadoReserva estado,
        LocalDateTime fechaReserva,
        boolean cancelable
) {
}
