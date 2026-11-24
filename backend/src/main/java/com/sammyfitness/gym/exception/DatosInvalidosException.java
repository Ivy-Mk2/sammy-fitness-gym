package com.sammyfitness.gym.exception;

/** 400: datos que pasan Bean Validation pero son inválidos (fecha pasada, horaFin &lt;= horaInicio, etc.). */
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
