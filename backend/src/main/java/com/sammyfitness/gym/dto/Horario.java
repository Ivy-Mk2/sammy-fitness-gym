package com.sammyfitness.gym.dto;

import java.time.LocalTime;

/** Franja horaria para filtrar clases según su hora de inicio. */
public enum Horario {
    MANANA(LocalTime.MIN, LocalTime.NOON),
    TARDE(LocalTime.NOON, LocalTime.of(18, 0)),
    NOCHE(LocalTime.of(18, 0), null);

    /** Inicio incluido. */
    private final LocalTime desde;
    /** Fin excluido (null = hasta el final del día). */
    private final LocalTime hasta;

    Horario(LocalTime desde, LocalTime hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public LocalTime getDesde() {
        return desde;
    }

    public LocalTime getHasta() {
        return hasta;
    }
}
