package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.Disciplina;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Alta/edición de clase. Si quien la crea es ENTRENADOR, entrenadorId puede omitirse
 * (se usa el propio entrenador); el ADMINISTRADOR debe indicarlo.
 */
public record ClaseRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
        String nombre,

        @NotNull(message = "La disciplina es obligatoria")
        Disciplina disciplina,

        Long entrenadorId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotNull(message = "El aforo es obligatorio")
        @Min(value = 1, message = "El aforo mínimo es 1")
        @Max(value = 50, message = "El aforo máximo es 50")
        Integer aforoMaximo,

        @Size(max = 200, message = "La descripción admite máximo 200 caracteres")
        String descripcion
) {
}
