package com.sammyfitness.gym.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reemplaza la página/JSON de error por defecto de Spring Boot (/error) para que los errores que
 * ocurren fuera de los controladores (filtros, contenedor) también tengan el formato
 * { fecha, codigo, mensaje, ruta }.
 */
@Hidden
@RestController
public class ErrorUniformeController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
        Object codigo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = codigo instanceof Integer c && HttpStatus.resolve(c) != null
                ? HttpStatus.resolve(c) : HttpStatus.INTERNAL_SERVER_ERROR;
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String ruta = uri != null ? uri.toString() : request.getRequestURI();

        String mensaje = switch (status) {
            case BAD_REQUEST -> "Petición inválida";
            case UNAUTHORIZED -> "No autenticado: inicie sesión para continuar";
            case FORBIDDEN -> GlobalExceptionHandler.SIN_PERMISO;
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "Método no permitido en esta ruta";
            default -> status.is5xxServerError() ? GlobalExceptionHandler.ERROR_INESPERADO : status.getReasonPhrase();
        };
        return ResponseEntity.status(status).body(ErrorResponse.de(status, mensaje, ruta));
    }
}
