package com.sammyfitness.gym.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/** Cuerpo uniforme de error: { fecha, codigo, mensaje, ruta }. */
public record ErrorResponse(LocalDateTime fecha, int codigo, String mensaje, String ruta) {

    public static ErrorResponse de(HttpStatus status, String mensaje, String ruta) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), mensaje, ruta);
    }
}
