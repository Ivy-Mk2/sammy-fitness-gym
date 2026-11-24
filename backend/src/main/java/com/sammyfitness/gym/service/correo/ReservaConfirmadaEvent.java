package com.sammyfitness.gym.service.correo;

import java.time.LocalDate;
import java.time.LocalTime;

/** Datos simples (no entidades JPA) para enviar el correo en otro hilo sin depender de la sesión. */
public record ReservaConfirmadaEvent(String correo, String nombreCliente, String clase, String entrenador,
                                     LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
}
