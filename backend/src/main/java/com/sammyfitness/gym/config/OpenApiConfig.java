package com.sammyfitness.gym.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * Para probar: ejecutar POST /api/auth/login, copiar el token y pulsar "Authorize".
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sammy Fitness Gym – API",
                version = "1.0",
                description = """
                        API del Sistema Web de Gestión de Clases y Membresías.

                        **Cómo probar:** ejecute `POST /api/auth/login` con un usuario de prueba, copie el `token`
                        y péguelo en el botón **Authorize** (sin escribir "Bearer").

                        Todos los errores tienen el formato `{ fecha, codigo, mensaje, ruta }`:
                        400 datos inválidos · 401 no autenticado · 403 sin permiso · 404 no existe ·
                        409 conflicto de negocio · 500 error inesperado."""),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtenido en POST /api/auth/login")
public class OpenApiConfig {
}
