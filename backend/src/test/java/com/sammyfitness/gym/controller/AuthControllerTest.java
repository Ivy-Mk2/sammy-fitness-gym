package com.sammyfitness.gym.controller;

import com.sammyfitness.gym.config.SecurityConfig;
import com.sammyfitness.gym.dto.LoginRequest;
import com.sammyfitness.gym.dto.LoginResponse;
import com.sammyfitness.gym.model.Rol;
import com.sammyfitness.gym.security.JwtAuthenticationFilter;
import com.sammyfitness.gym.security.JwtService;
import com.sammyfitness.gym.security.RestAuthenticationHandlers;
import com.sammyfitness.gym.security.UsuarioDetailsService;
import com.sammyfitness.gym.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la capa web de AuthController con MockMvc (@WebMvcTest): se carga solo el controlador,
 * la configuración de seguridad y el manejador de errores; AuthService se simula.
 */
@WebMvcTest(controllers = AuthController.class, properties = "app.cors.allowed-origin=http://localhost:4200")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationHandlers.class})
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UsuarioDetailsService usuarioDetailsService;

    private static final String BODY_VALIDO = """
            {"correo":"admin@sammyfitness.test","password":"Secreta123"}""";

    @Test
    @DisplayName("PE-01: login válido → 200 con token, rol y nombre (sin enviar token)")
    void loginValido() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("jwt.de.prueba", Rol.ADMINISTRADOR, "Sandra Medina"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.de.prueba"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.nombre").value("Sandra Medina"));
        verify(authService).login(new LoginRequest("admin@sammyfitness.test", "Secreta123"));
    }

    @Test
    @DisplayName("PE-02: contraseña incorrecta → 401 'Credenciales incorrectas'")
    void credencialesIncorrectas() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"))
                .andExpect(jsonPath("$.ruta").value("/api/auth/login"))
                .andExpect(jsonPath("$.fecha").exists());
    }

    @Test
    @DisplayName("Usuario desactivado → mismo 401 genérico (no revela el motivo)")
    void usuarioDesactivado() throws Exception {
        when(authService.login(any())).thenThrow(new DisabledException("User is disabled"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    @DisplayName("Correo inválido o contraseña vacía → 400 sin llamar al servicio")
    void datosInvalidos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"no-es-correo","password":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("JSON mal formado → 400")
    void jsonMalFormado() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{correo:"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }
}
