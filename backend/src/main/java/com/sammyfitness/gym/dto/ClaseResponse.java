package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.Disciplina;

import java.time.LocalDate;
import java.time.LocalTime;

public record ClaseResponse(
        Long id,
        String nombre,
        Disciplina disciplina,
        Long entrenadorId,
        String entrenadorNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        int aforoMaximo,
        long cuposOcupados,
        long cuposDisponibles,
        String descripcion
) {
}
