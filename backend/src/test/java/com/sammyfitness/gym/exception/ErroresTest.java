package com.sammyfitness.gym.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fase 5: todos los errores responden con { fecha, codigo, mensaje, ruta } y el código HTTP correcto. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Import(ErroresTest.ControladorQueFalla.class)
@Transactional
class ErroresTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @TestConfiguration
    @RestController
    static class ControladorQueFalla {
        @GetMapping("/api/test/explota")
        String explota() {
            throw new IllegalStateException("detalle interno que no debe filtrarse: tabla usuarios");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private String token(String correo) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("correo", correo, "password", "Test12345"))))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private MockHttpServletRequestBuilder como(String correo, MockHttpServletRequestBuilder req) throws Exception {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(correo));
    }

    /** Verifica el código HTTP y el formato uniforme del cuerpo. */
    private ResultActions esperar(ResultActions r, int codigo, String ruta) throws Exception {
        return r.andExpect(status().is(codigo))
                .andExpect(jsonPath("$.fecha").isNotEmpty())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.ruta").value(ruta));
    }

    private Map<String, Object> claseValida() {
        Map<String, Object> c = new HashMap<>();
        c.put("nombre", "Zumba Test");
        c.put("disciplina", "ZUMBA");
        c.put("fecha", LocalDate.now().plusDays(8).toString());
        c.put("horaInicio", "20:00");
        c.put("horaFin", "21:00");
        c.put("aforoMaximo", 20);
        return c;
    }

    private ResultActions crearClase(Object body) throws Exception {
        return mvc.perform(como("carlos.ramos@sammyfitness.test", post("/api/clases"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    // ------------------------------------------------------------------ 400

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        esperar(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{correo: ")),
                400, "/api/auth/login")
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la petición no es un JSON válido"));
    }

    @Test
    void enumInvalidoEnElCuerpoIndicaLosValoresPermitidos() throws Exception {
        Map<String, Object> body = claseValida();
        body.put("disciplina", "BOXEO");
        esperar(crearClase(body), 400, "/api/clases")
                .andExpect(jsonPath("$.mensaje", allOf(containsString("disciplina"), containsString("BOXEO"),
                        containsString("SPINNING"), containsString("HALTEROFILIA"))));
    }

    @Test
    void fechaConFormatoIncorrectoDevuelve400() throws Exception {
        Map<String, Object> body = claseValida();
        body.put("fecha", "31/12/2026");
        esperar(crearClase(body), 400, "/api/clases")
                .andExpect(jsonPath("$.mensaje", containsString("yyyy-MM-dd")));
    }

    @Test
    void beanValidationReportaTodosLosCamposInvalidos() throws Exception {
        Map<String, Object> body = claseValida();
        body.put("nombre", "AB");
        body.put("aforoMaximo", 0);
        body.put("descripcion", "x".repeat(201));
        body.remove("disciplina");
        esperar(crearClase(body), 400, "/api/clases")
                .andExpect(jsonPath("$.mensaje", allOf(containsString("nombre"), containsString("aforoMaximo"),
                        containsString("descripcion"), containsString("disciplina"))));
    }

    @Test
    void fechaPasadaYHorarioInvertidoSeValidanEnElDto() throws Exception {
        Map<String, Object> body = claseValida();
        body.put("fecha", LocalDate.now().minusDays(1).toString());
        body.put("horaInicio", "21:00");
        body.put("horaFin", "20:00");
        esperar(crearClase(body), 400, "/api/clases")
                .andExpect(jsonPath("$.mensaje", allOf(containsString("no puede ser pasada"),
                        containsString("hora de fin debe ser posterior"))));
    }

    @Test
    void validacionesDeSocio() throws Exception {
        Map<String, Object> socio = Map.of("nombres", "", "apellidos", "Ruiz", "dni", "1234567A",
                "correo", "sin-arroba", "telefono", "98765432", "password", "corta");
        esperar(mvc.perform(como("admin@sammyfitness.test", post("/api/socios"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(socio))),
                400, "/api/socios")
                .andExpect(jsonPath("$.mensaje", allOf(containsString("nombres"), containsString("8 dígitos"),
                        containsString("correo"), containsString("9 dígitos"), containsString("contraseña"))));
    }

    @Test
    void parametroDeConsultaInvalidoDevuelve400() throws Exception {
        esperar(mvc.perform(como("cliente01@correo.test", get("/api/clases").param("horario", "MADRUGADA"))),
                400, "/api/clases")
                .andExpect(jsonPath("$.mensaje", allOf(containsString("horario"), containsString("MANANA"))));
    }

    @Test
    void idNoNumericoDevuelve400() throws Exception {
        esperar(mvc.perform(como("cliente01@correo.test", get("/api/clases/abc"))), 400, "/api/clases/abc")
                .andExpect(jsonPath("$.mensaje", containsString("número entero")));
    }

    // ------------------------------------------------------------------ 401 / 403 / 404

    @Test
    void sinTokenDevuelve401Uniforme() throws Exception {
        esperar(mvc.perform(get("/api/planes")), 401, "/api/planes");
    }

    @Test
    void sinPermisoDevuelve403Uniforme() throws Exception {
        esperar(mvc.perform(como("cliente01@correo.test", get("/api/socios"))), 403, "/api/socios");
    }

    @Test
    void recursoInexistenteDevuelve404() throws Exception {
        esperar(mvc.perform(como("admin@sammyfitness.test", get("/api/socios/9999"))), 404, "/api/socios/9999");
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        esperar(mvc.perform(como("admin@sammyfitness.test", get("/api/no-existe"))), 404, "/api/no-existe");
    }

    // ------------------------------------------------------------------ 405 / 409 / 415 / 500

    @Test
    void metodoNoPermitidoDevuelve405() throws Exception {
        esperar(mvc.perform(como("admin@sammyfitness.test", patch("/api/planes"))), 405, "/api/planes");
    }

    @Test
    void conflictoDeNegocioDevuelve409() throws Exception {
        Map<String, Object> socio = Map.of("nombres", "Dup", "apellidos", "Correo", "dni", "40000001",
                "correo", "CLIENTE01@correo.test", "telefono", "987654321", "password", "Clave12345");
        esperar(mvc.perform(como("recepcion@sammyfitness.test", post("/api/socios"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(socio))),
                409, "/api/socios")
                .andExpect(jsonPath("$.mensaje", containsString("correo")));
    }

    @Test
    void tipoDeContenidoNoSoportadoDevuelve415() throws Exception {
        esperar(mvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("hola")),
                415, "/api/auth/login");
    }

    @Test
    void errorInesperadoDevuelve500SinFiltrarDetalles() throws Exception {
        esperar(mvc.perform(como("admin@sammyfitness.test", get("/api/test/explota"))), 500, "/api/test/explota")
                .andExpect(jsonPath("$.mensaje", not(containsString("tabla"))))
                .andExpect(jsonPath("$.mensaje", containsString("error inesperado")));
    }

    @Test
    void erroresFueraDeLosControladoresTambienSonUniformes() throws Exception {
        esperar(mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/ruta/rara")),
                404, "/ruta/rara");
    }
}
