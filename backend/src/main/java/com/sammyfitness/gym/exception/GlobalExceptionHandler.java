package com.sammyfitness.gym.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Manejo centralizado de errores. En la Fase 5 se completa con 404, 409 y 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String CREDENCIALES_INCORRECTAS = "Credenciales incorrectas";

    /** Login fallido: mensaje genérico, sin indicar si falló el correo o la contraseña. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacion(AuthenticationException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, CREDENCIALES_INCORRECTAS, req);
    }

    /** @PreAuthorize denegado. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, req);
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status, mensaje, req.getRequestURI()));
    }
}
