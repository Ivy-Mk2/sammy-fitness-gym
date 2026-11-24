package com.sammyfitness.gym.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la Fase 4: rutas, roles y códigos HTTP de la API REST (incluye los casos PE de la Fase 6).
 * Usa los datos ficticios del DataSeeder; cada prueba hace rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Transactional
class ApiRestTest {

    private static final String PASSWORD = "Test12345";
    private static final String ADMIN = "admin@sammyfitness.test";
    private static final String RECEPCION = "recepcion@sammyfitness.test";
    private static final String ENTRENADOR = "carlos.ramos@sammyfitness.test";
    private static final String CLIENTE_01 = "cliente01@correo.test";
    private static final String CLIENTE_03 = "cliente03@correo.test";
    private static final String CLIENTE_VENCIDO = "cliente10@correo.test";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClaseRepository claseRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ReservaRepository reservaRepository;
    @Autowired MembresiaRepository membresiaRepository;
    @Autowired PlanRepository planRepository;

    // ------------------------------------------------------------------ utilidades

    private String token(String correo) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("correo", correo, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private MockHttpServletRequestBuilder como(String correo, MockHttpServletRequestBuilder req) throws Exception {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(correo));
    }

    private MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private Long idClase(String nombre) {
        return claseRepository.findAll().stream().filter(c -> c.getNombre().equals(nombre))
                .findFirst().orElseThrow().getId();
    }

    private Usuario usuario(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
    }

    private Map<String, Object> nuevaClase(Long entrenadorId) {
        var body = new java.util.HashMap<String, Object>();
        body.put("nombre", "Funcional Express");
        body.put("disciplina", "FUNCIONAL");
        body.put("fecha", LocalDate.now().plusDays(6).toString());
        body.put("horaInicio", "06:00");
        body.put("horaFin", "06:45");
        body.put("aforoMaximo", 15);
        body.put("descripcion", "Clase corta antes del trabajo");
        if (entrenadorId != null) {
            body.put("entrenadorId", entrenadorId);
        }
        return body;
    }

    // ------------------------------------------------------------------ Clases

    @Test
    void pe03_listarClasesSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/clases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    void listarClasesDevuelveElCatalogoConCupos() throws Exception {
        mvc.perform(como(CLIENTE_01, get("/api/clases")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].cuposDisponibles").exists())
                .andExpect(jsonPath("$[0].entrenadorNombre").isNotEmpty())
                // la API expone DTO: nunca la entidad (sin password ni version)
                .andExpect(jsonPath("$[0].entrenador").doesNotExist())
                .andExpect(jsonPath("$[0].version").doesNotExist());
    }

    @Test
    void listarClasesConFiltros() throws Exception {
        mvc.perform(como(CLIENTE_01, get("/api/clases").param("disciplina", "YOGA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].disciplina", everyItem(is("YOGA"))));
        mvc.perform(como(CLIENTE_01, get("/api/clases").param("horario", "NOCHE")
                        .param("dia", LocalDate.now().plusDays(1).toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void pe10_claseInexistenteDevuelve404() throws Exception {
        mvc.perform(como(CLIENTE_01, get("/api/clases/9999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.ruta").value("/api/clases/9999"));
    }

    @Test
    void pe04_clienteNoPuedeCrearClases() throws Exception {
        mvc.perform(conJson(como(CLIENTE_01, post("/api/clases")), nuevaClase(null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    void pe05_entrenadorCreaClase() throws Exception {
        mvc.perform(conJson(como(ENTRENADOR, post("/api/clases")), nuevaClase(null)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/clases/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.entrenadorId").value(usuario(ENTRENADOR).getId()))
                .andExpect(jsonPath("$.cuposDisponibles").value(15));
    }

    @Test
    void crearClaseConDatosInvalidosDevuelve400() throws Exception {
        Map<String, Object> body = nuevaClase(null);
        body.put("nombre", "AB");
        body.put("aforoMaximo", 80);
        mvc.perform(conJson(como(ENTRENADOR, post("/api/clases")), body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", allOf(containsString("nombre"), containsString("aforoMaximo"))));
    }

    @Test
    void administradorEditaYEliminaClase() throws Exception {
        Long entrenadorId = usuario(ENTRENADOR).getId();
        String creada = mvc.perform(conJson(como(ADMIN, post("/api/clases")), nuevaClase(entrenadorId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("id").asLong();

        Map<String, Object> cambios = nuevaClase(entrenadorId);
        cambios.put("aforoMaximo", 30);
        mvc.perform(conJson(como(ADMIN, put("/api/clases/" + id)), cambios))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aforoMaximo").value(30));

        mvc.perform(como(ADMIN, delete("/api/clases/" + id))).andExpect(status().isNoContent());
        mvc.perform(como(ADMIN, get("/api/clases/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void inscritosSegunRol() throws Exception {
        Long spinning = idClase("Spinning Power");
        mvc.perform(como(RECEPCION, get("/api/clases/" + spinning + "/inscritos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nombreCompleto").isNotEmpty());
        mvc.perform(como(CLIENTE_01, get("/api/clases/" + spinning + "/inscritos")))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ Reservas

    @Test
    void pe06_reservaValidaDevuelve201() throws Exception {
        mvc.perform(conJson(como(CLIENTE_03, post("/api/reservas")), Map.of("claseId", idClase("Yoga Flow"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.claseNombre").value("Yoga Flow"))
                .andExpect(jsonPath("$.cancelable").value(true));
    }

    @Test
    void pe07_claseLlenaDevuelve409() throws Exception {
        mvc.perform(conJson(como(CLIENTE_03, post("/api/reservas")), Map.of("claseId", idClase("Halterofilia Técnica"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje", containsString("aforo completo")));
    }

    @Test
    void pe08_membresiaVencidaDevuelve409() throws Exception {
        mvc.perform(conJson(como(CLIENTE_VENCIDO, post("/api/reservas")), Map.of("claseId", idClase("Yoga Flow"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("membresía activa")));
    }

    @Test
    void reservaSinClaseDevuelve400() throws Exception {
        mvc.perform(conJson(como(CLIENTE_03, post("/api/reservas")), Map.of()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soloClientesReservan() throws Exception {
        mvc.perform(conJson(como(RECEPCION, post("/api/reservas")), Map.of("claseId", idClase("Yoga Flow"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void misReservasDelCliente() throws Exception {
        mvc.perform(como(CLIENTE_01, get("/api/reservas/mis-reservas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].estado", everyItem(is("CONFIRMADA"))));
    }

    @Test
    void pe09_cancelarReservaPropiaDevuelve200() throws Exception {
        Long reservaId = reservaRepository.findDelCliente(usuario(CLIENTE_01).getId()).get(0).getId();

        mvc.perform(como(CLIENTE_01, delete("/api/reservas/" + reservaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelable").value(false));
    }

    @Test
    void cancelarReservaAjenaDevuelve403() throws Exception {
        Long reservaId = reservaRepository.findDelCliente(usuario(CLIENTE_01).getId()).get(0).getId();

        mvc.perform(como(CLIENTE_03, delete("/api/reservas/" + reservaId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void asistenciaDeClaseFuturaDevuelve409YClienteNoPuedeMarcarla() throws Exception {
        Long reservaId = reservaRepository.findDelCliente(usuario(CLIENTE_01).getId()).get(0).getId();

        mvc.perform(conJson(como(ADMIN, patch("/api/reservas/" + reservaId + "/asistencia")), Map.of("asistio", true)))
                .andExpect(status().isConflict());
        mvc.perform(conJson(como(CLIENTE_01, patch("/api/reservas/" + reservaId + "/asistencia")), Map.of("asistio", true)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ Membresías, socios y planes

    @Test
    void miMembresiaSoloParaClientes() throws Exception {
        mvc.perform(como(CLIENTE_01, get("/api/membresias/mi-membresia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.planNombre").value("Mensual"))
                .andExpect(jsonPath("$.diasRestantes").value(25));
        mvc.perform(como(ADMIN, get("/api/membresias/mi-membresia")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sociosSoloParaAdministradorYRecepcion() throws Exception {
        mvc.perform(como(RECEPCION, get("/api/socios")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(como(RECEPCION, get("/api/socios").param("q", "cliente10")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].membresiaActual.estado").value("VENCIDA"));
        mvc.perform(como(ENTRENADOR, get("/api/socios"))).andExpect(status().isForbidden());
        mvc.perform(como(CLIENTE_01, get("/api/socios"))).andExpect(status().isForbidden());
    }

    @Test
    void registrarSocioYAsignarMembresia() throws Exception {
        var socio = Map.of("nombres", "Rocío", "apellidos", "Paredes Luna", "dni", "45612378",
                "correo", "rocio@correo.test", "telefono", "987123456", "password", "Clave2026!");
        String creado = mvc.perform(conJson(como(RECEPCION, post("/api/socios")), socio))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.membresiaActual").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        long socioId = json.readTree(creado).get("id").asLong();
        Long trimestral = planRepository.findByNombreIgnoreCase("Trimestral").orElseThrow().getId();
        String inicio = LocalDate.now().toString();

        mvc.perform(conJson(como(RECEPCION, post("/api/socios/" + socioId + "/membresias")),
                        Map.of("planId", trimestral, "fechaInicio", inicio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaVencimiento").value(LocalDate.now().plusDays(90).toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void socioConDniDuplicadoDevuelve409() throws Exception {
        var socio = Map.of("nombres", "Copia", "apellidos", "Duplicada", "dni", usuario(CLIENTE_01).getDni(),
                "correo", "otro@correo.test", "telefono", "987123456", "password", "Clave2026!");
        mvc.perform(conJson(como(ADMIN, post("/api/socios")), socio))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("DNI")));
    }

    @Test
    void socioConDatosInvalidosDevuelve400() throws Exception {
        var socio = Map.of("nombres", "X", "apellidos", "Y", "dni", "123", "correo", "no-es-correo",
                "telefono", "12", "password", "Clave2026!");
        mvc.perform(conJson(como(ADMIN, post("/api/socios")), socio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", allOf(containsString("dni"), containsString("telefono"),
                        containsString("correo"))));
    }

    @Test
    void cambiarEstadoDeMembresia() throws Exception {
        Long membresiaId = membresiaRepository.findByUsuarioIdOrderByFechaInicioDesc(usuario(CLIENTE_03).getId())
                .get(0).getId();

        mvc.perform(conJson(como(ADMIN, patch("/api/membresias/" + membresiaId + "/estado")), Map.of("estado", "CONGELADA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONGELADA"));
        mvc.perform(conJson(como(CLIENTE_03, patch("/api/membresias/" + membresiaId + "/estado")), Map.of("estado", "ACTIVA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void planesParaCualquierAutenticado() throws Exception {
        mvc.perform(como(CLIENTE_01, get("/api/planes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Mensual"));
    }

    // ------------------------------------------------------------------ Swagger

    @Test
    void documentacionOpenApiEsPublica() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/reservas']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }
}
