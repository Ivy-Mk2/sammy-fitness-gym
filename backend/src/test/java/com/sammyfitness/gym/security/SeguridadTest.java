package com.sammyfitness.gym.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sammyfitness.gym.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la Fase 2: login, filtro JWT, @PreAuthorize por rol y CORS.
 * Usa los datos ficticios del DataSeeder (contraseña de pruebas "Test12345").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Import(SeguridadTest.EndpointsDePrueba.class)
class SeguridadTest {

    private static final String PASSWORD = "Test12345";
    private static final String ADMIN = "admin@sammyfitness.test";
    private static final String CLIENTE = "cliente01@correo.test";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;

    /** Endpoints solo para estas pruebas (los reales se crean en la Fase 4). */
    @TestConfiguration
    @RestController
    static class EndpointsDePrueba {
        @GetMapping("/api/test/perfil")
        Map<String, Object> perfil(@AuthenticationPrincipal UsuarioPrincipal u) {
            return Map.of("correo", u.correo(), "rol", u.rol().name());
        }

        @GetMapping("/api/test/solo-admin")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        Map<String, String> soloAdmin() {
            return Map.of("ok", "si");
        }
    }

    private String loginBody(String correo, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("correo", correo, "password", password));
    }

    private String obtenerToken(String correo) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(correo, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    // ---------- Login ----------

    @Test
    void loginValidoDevuelveTokenRolYNombre() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.nombre").value("Sandra Medina Paredes"));
    }

    @Test
    void loginConContraseniaIncorrectaDevuelve401Generico() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN, "incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"))
                .andExpect(jsonPath("$.ruta").value("/api/auth/login"))
                .andExpect(jsonPath("$.fecha").exists());
    }

    @Test
    void loginConCorreoInexistenteDevuelveElMismoMensaje() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("noexiste@correo.test", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    @Transactional
    void loginDeUsuarioInactivoDevuelve401Generico() throws Exception {
        var cliente = usuarioRepository.findByCorreoIgnoreCase("cliente05@correo.test").orElseThrow();
        cliente.setActivo(false);
        usuarioRepository.saveAndFlush(cliente);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("cliente05@correo.test", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
    }

    @Test
    void loginConDatosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("no-es-correo", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    // ---------- Filtro JWT ----------

    @Test
    void rutaProtegidaSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/test/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.ruta").value("/api/test/perfil"));
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/test/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoIdentificaAlUsuario() throws Exception {
        mvc.perform(get("/api/test/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerToken(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(CLIENTE))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    // ---------- @PreAuthorize ----------

    @Test
    void clienteNoPuedeAccederARutaDeAdministrador() throws Exception {
        mvc.perform(get("/api/test/solo-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerToken(CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    void administradorPuedeAccederARutaDeAdministrador() throws Exception {
        mvc.perform(get("/api/test/solo-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerToken(ADMIN)))
                .andExpect(status().isOk());
    }

    // ---------- CORS ----------

    @Test
    void corsPermiteElOrigenDelFrontend() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void corsRechazaOtrosOrigenes() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
