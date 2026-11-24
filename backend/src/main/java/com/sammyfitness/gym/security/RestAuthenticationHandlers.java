package com.sammyfitness.gym.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sammyfitness.gym.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respuestas JSON uniformes para 401 (no autenticado) y 403 (sin permiso) del filtro de seguridad. */
@Component
@RequiredArgsConstructor
public class RestAuthenticationHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        escribir(response, request, HttpStatus.UNAUTHORIZED, "No autenticado: inicie sesión para continuar");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escribir(response, request, HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción");
    }

    private void escribir(HttpServletResponse response, HttpServletRequest request,
                          HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.de(status, mensaje, request.getRequestURI()));
    }
}
