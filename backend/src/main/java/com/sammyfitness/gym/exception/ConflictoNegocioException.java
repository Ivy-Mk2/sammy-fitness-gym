package com.sammyfitness.gym.exception;

/** 409: la operación viola una regla de negocio (aforo completo, duplicado, membresía no activa, etc.). */
public class ConflictoNegocioException extends RuntimeException {

    public ConflictoNegocioException(String mensaje) {
        super(mensaje);
    }
}
