package com.sammyfitness.gym.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Manejo centralizado de errores. Todas las respuestas usan el cuerpo uniforme
 * { fecha, codigo, mensaje, ruta }:
 * 400 datos inválidos · 401 no autenticado · 403 sin permiso · 404 no existe ·
 * 409 conflicto de negocio · 500 error inesperado.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String CREDENCIALES_INCORRECTAS = "Credenciales incorrectas";
    static final String SIN_PERMISO = "No tiene permiso para realizar esta acción";
    static final String CONFLICTO_CONCURRENCIA =
            "Otra operación modificó la clase al mismo tiempo. Vuelva a intentarlo.";
    static final String ERROR_INESPERADO =
            "Ocurrió un error inesperado. Intente nuevamente o contacte al administrador.";

    // ============================================================ 400 – datos inválidos

    /** Bean Validation sobre el cuerpo (@Valid @RequestBody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        String global = ex.getBindingResult().getGlobalErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, unir(mensaje, global), req);
    }

    /** Bean Validation sobre parámetros (@RequestParam, @PathVariable con restricciones). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionParametros(HandlerMethodValidationException ex, HttpServletRequest req) {
        String mensaje = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restricciones(ConstraintViolationException ex, HttpServletRequest req) {
        String mensaje = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, req);
    }

    /** Reglas que pasan Bean Validation pero son inválidas (fecha/hora pasada, entrenador inexistente…). */
    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(DatosInvalidosException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    /** JSON mal formado o con un valor de tipo incorrecto (p. ej. disciplina "BOXEO", fecha "31/12"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String mensaje = "El cuerpo de la petición no es un JSON válido";
        if (ex.getCause() instanceof InvalidFormatException ife) {
            mensaje = valorInvalido(campo(ife), String.valueOf(ife.getValue()), ife.getTargetType());
        } else if (ex.getCause() instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
            mensaje = campo(mie) + ": tipo de dato incorrecto";
        }
        return responder(HttpStatus.BAD_REQUEST, mensaje, req);
    }

    /** Parámetro de la URL con tipo incorrecto (p. ej. /api/clases/abc o ?horario=MADRUGADA). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoParametro(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST,
                valorInvalido(ex.getName(), String.valueOf(ex.getValue()), ex.getRequiredType()), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, ex.getParameterName() + ": es obligatorio", req);
    }

    // ============================================================ 401 / 403

    /** Login fallido: mensaje genérico, sin indicar si falló el correo o la contraseña. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacion(AuthenticationException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, CREDENCIALES_INCORRECTAS, req);
    }

    /** @PreAuthorize denegado o recurso ajeno (p. ej. cancelar la reserva de otro cliente). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, SIN_PERMISO, req);
    }

    // ============================================================ 404

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    /** Ruta inexistente. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", req);
    }

    // ============================================================ 405 / 415

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED,
                "Método " + ex.getMethod() + " no permitido en esta ruta", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoContenido(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envíe el cuerpo como application/json", req);
    }

    // ============================================================ 409 – conflicto de negocio

    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoNegocioException ex, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    /** RN-02: dos operaciones simultáneas sobre la misma clase (@Version). */
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> concurrencia(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, CONFLICTO_CONCURRENCIA, req);
    }

    /** Restricción única de la BD (p. ej. DNI/correo registrados en paralelo). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "El registro entra en conflicto con datos existentes", req);
    }

    // ============================================================ Otros / 500

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> conEstado(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatusCode codigo = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(codigo.value());
        return responder(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getReason() != null ? ex.getReason() : ERROR_INESPERADO, req);
    }

    /** Cualquier error no previsto: se registra completo en el log, pero al cliente no se le filtran detalles. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_INESPERADO, req);
    }

    // ============================================================ apoyo

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status, mensaje, req.getRequestURI()));
    }

    private static String campo(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(r -> r.getFieldName() != null ? r.getFieldName() : "[" + r.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private static String valorInvalido(String campo, String valor, Class<?> tipo) {
        String base = campo + ": valor inválido '" + valor + "'";
        if (tipo != null && tipo.isEnum()) {
            return base + ". Valores permitidos: " + Arrays.stream(tipo.getEnumConstants())
                    .map(Object::toString).collect(Collectors.joining(", "));
        }
        if (tipo != null) {
            return base + switch (tipo.getSimpleName()) {
                case "LocalDate" -> " (formato yyyy-MM-dd)";
                case "LocalTime" -> " (formato HH:mm)";
                case "Long", "Integer", "long", "int" -> " (debe ser un número entero)";
                case "Boolean", "boolean" -> " (debe ser true o false)";
                default -> "";
            };
        }
        return base;
    }

    private static String unir(String a, String b) {
        if (a.isBlank()) return b;
        if (b.isBlank()) return a;
        return a + "; " + b;
    }
}
