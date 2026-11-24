package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.Disciplina;

import java.time.LocalDate;

/**
 * Filtros opcionales del catálogo de clases.
 *
 * @param dia     fecha exacta (yyyy-MM-dd). Si no se envía, se listan las clases desde hoy.
 * @param horario MANANA (antes de 12:00), TARDE (12:00–17:59) o NOCHE (desde 18:00).
 */
public record ClaseFiltro(Disciplina disciplina, LocalDate dia, Horario horario, Long entrenadorId) {

    public static ClaseFiltro vacio() {
        return new ClaseFiltro(null, null, null, null);
    }
}
