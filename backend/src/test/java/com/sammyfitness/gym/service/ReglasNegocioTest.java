package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.*;
import com.sammyfitness.gym.exception.ConflictoNegocioException;
import com.sammyfitness.gym.exception.DatosInvalidosException;
import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.MembresiaRepository;
import com.sammyfitness.gym.repository.ReservaRepository;
import com.sammyfitness.gym.repository.UsuarioRepository;
import com.sammyfitness.gym.service.correo.NotificacionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.sammyfitness.gym.service.DatosPrueba.principal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/** Pruebas de integración (H2) de las reglas de negocio de la Fase 3. Cada prueba hace rollback. */
@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Import(DatosPrueba.class)
@Transactional
class ReglasNegocioTest {

    @Autowired DatosPrueba datos;
    @Autowired ReservaService reservaService;
    @Autowired ClaseService claseService;
    @Autowired ClienteMembresiaService membresiaService;
    @Autowired ReservaRepository reservaRepository;
    @Autowired MembresiaRepository membresiaRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @MockitoBean NotificacionService notificaciones;

    static final LocalDate MANANA = LocalDate.now().plusDays(1);

    // =====================================================================================
    @Nested
    class Reservas {

        @Test
        void reservaValidaQuedaConfirmadaYOcupaUnCupo() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
            Clase clase = datos.clase(entrenador, MANANA, "10:00", "11:00", 5);
            Usuario cliente = datos.clienteConMembresia();

            ReservaResponse r = reservaService.reservar(cliente.getId(), clase.getId());

            assertThat(r.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(r.cancelable()).isTrue();
            assertThat(claseService.obtener(clase.getId()).cuposDisponibles()).isEqualTo(4);
        }

        @Test
        void rn01_clienteSinMembresiaNoPuedeReservar() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Usuario cliente = datos.usuario(Rol.CLIENTE);

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("membresía activa");
        }

        @Test
        void rn01_membresiaVencidaNoPermiteReservar() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Usuario cliente = datos.usuario(Rol.CLIENTE);
            datos.membresia(cliente, LocalDate.now().minusDays(40), EstadoMembresia.VENCIDA);

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void rn01_membresiaCongeladaNoPermiteReservar() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Usuario cliente = datos.usuario(Rol.CLIENTE);
            datos.membresia(cliente, LocalDate.now(), EstadoMembresia.CONGELADA);

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void rn01_laMembresiaDebeCubrirLaFechaDeLaClase() {
            // Vence en 2 días; la clase es dentro de 5 días
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), LocalDate.now().plusDays(5), "10:00", "11:00", 5);
            Usuario cliente = datos.usuario(Rol.CLIENTE);
            datos.membresia(cliente, LocalDate.now().minusDays(28), EstadoMembresia.ACTIVA);

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void rn02_claseLlenaRechazaLaReserva() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 1);
            reservaService.reservar(datos.clienteConMembresia().getId(), clase.getId());
            Usuario otro = datos.clienteConMembresia();

            assertThatThrownBy(() -> reservaService.reservar(otro.getId(), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("aforo completo");
        }

        @Test
        void rn03_noSePuedeReservarDosVecesLaMismaClase() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Usuario cliente = datos.clienteConMembresia();
            reservaService.reservar(cliente.getId(), clase.getId());

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("Ya tiene una reserva");
        }

        @Test
        void rn03_noSePuedenReservarDosClasesQueSeCruzan() {
            Clase c1 = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Clase c2 = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:30", "11:30", 5);
            Usuario cliente = datos.clienteConMembresia();
            reservaService.reservar(cliente.getId(), c1.getId());

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), c2.getId()))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("mismo horario");
        }

        @Test
        void rn03_clasesConsecutivasSiSePermiten() {
            Clase c1 = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Clase c2 = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "11:00", "12:00", 5);
            Usuario cliente = datos.clienteConMembresia();
            reservaService.reservar(cliente.getId(), c1.getId());

            assertThat(reservaService.reservar(cliente.getId(), c2.getId()).estado())
                    .isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        void rn04_cancelarLiberaElCupo() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 1);
            Usuario cliente = datos.clienteConMembresia();
            ReservaResponse r = reservaService.reservar(cliente.getId(), clase.getId());

            ReservaResponse cancelada = reservaService.cancelar(cliente.getId(), r.id());

            assertThat(cancelada.estado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(claseService.obtener(clase.getId()).cuposDisponibles()).isEqualTo(1);
            // otro cliente ya puede ocupar el cupo
            assertThat(reservaService.reservar(datos.clienteConMembresia().getId(), clase.getId()).estado())
                    .isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        void rn04_trasCancelarElClientePuedeVolverAReservar() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Usuario cliente = datos.clienteConMembresia();
            reservaService.cancelar(cliente.getId(), reservaService.reservar(cliente.getId(), clase.getId()).id());

            assertThat(reservaService.reservar(cliente.getId(), clase.getId()).estado())
                    .isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        void rn04_noSePuedeCancelarUnaClaseYaRealizada() {
            Usuario cliente = datos.clienteConMembresia();
            Clase pasada = datos.clase(datos.usuario(Rol.ENTRENADOR), LocalDate.now().minusDays(1), "10:00", "11:00", 5);
            Reserva r = datos.reserva(cliente, pasada, EstadoReserva.CONFIRMADA);

            assertThatThrownBy(() -> reservaService.cancelar(cliente.getId(), r.getId()))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("ya se realizó");
        }

        @Test
        void rn04_soloSeCancelanLasReservasPropias() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            ReservaResponse r = reservaService.reservar(datos.clienteConMembresia().getId(), clase.getId());
            Usuario intruso = datos.clienteConMembresia();

            assertThatThrownBy(() -> reservaService.cancelar(intruso.getId(), r.id()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void noSePuedeReservarUnaClaseQueYaPaso() {
            Clase pasada = datos.clase(datos.usuario(Rol.ENTRENADOR), LocalDate.now().minusDays(1), "10:00", "11:00", 5);
            Usuario cliente = datos.usuario(Rol.CLIENTE);
            datos.membresia(cliente, LocalDate.now().minusDays(5), EstadoMembresia.ACTIVA);

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), pasada.getId()))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void asistenciaSoloDesdeElDiaDeLaClase() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
            Usuario cliente = datos.clienteConMembresia();
            Reserva futura = datos.reserva(cliente, datos.clase(entrenador, MANANA, "10:00", "11:00", 5), EstadoReserva.CONFIRMADA);
            Reserva pasada = datos.reserva(cliente,
                    datos.clase(entrenador, LocalDate.now().minusDays(1), "10:00", "11:00", 5), EstadoReserva.CONFIRMADA);

            assertThatThrownBy(() -> reservaService.marcarAsistencia(principal(entrenador), futura.getId(), true))
                    .isInstanceOf(ConflictoNegocioException.class);
            assertThat(reservaService.marcarAsistencia(principal(entrenador), pasada.getId(), true).estado())
                    .isEqualTo(EstadoReserva.ASISTIDA);
        }

        @Test
        void entrenadorNoMarcaAsistenciaEnClasesAjenas() {
            Usuario cliente = datos.clienteConMembresia();
            Reserva r = datos.reserva(cliente,
                    datos.clase(datos.usuario(Rol.ENTRENADOR), LocalDate.now().minusDays(1), "10:00", "11:00", 5),
                    EstadoReserva.CONFIRMADA);
            Usuario otroEntrenador = datos.usuario(Rol.ENTRENADOR);

            assertThatThrownBy(() -> reservaService.marcarAsistencia(principal(otroEntrenador), r.getId(), true))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // =====================================================================================
    @Nested
    class Clases {

        private ClaseRequest req(LocalDate fecha, String inicio, String fin, int aforo, Long entrenadorId) {
            return new ClaseRequest("Spinning Test", Disciplina.SPINNING, entrenadorId, fecha,
                    LocalTime.parse(inicio), LocalTime.parse(fin), aforo, "Descripción");
        }

        @Test
        void entrenadorCreaClaseParaSiMismo() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);

            ClaseResponse c = claseService.crear(principal(entrenador), req(MANANA, "06:00", "07:00", 20, null));

            assertThat(c.entrenadorId()).isEqualTo(entrenador.getId());
            assertThat(c.cuposDisponibles()).isEqualTo(20);
        }

        @Test
        void entrenadorNoPuedeCrearClasesParaOtro() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
            Usuario otro = datos.usuario(Rol.ENTRENADOR);

            assertThatThrownBy(() -> claseService.crear(principal(entrenador), req(MANANA, "06:00", "07:00", 20, otro.getId())))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void administradorDebeIndicarUnEntrenadorValido() {
            Usuario admin = datos.usuario(Rol.ADMINISTRADOR);
            Usuario cliente = datos.usuario(Rol.CLIENTE);

            assertThatThrownBy(() -> claseService.crear(principal(admin), req(MANANA, "06:00", "07:00", 20, null)))
                    .isInstanceOf(DatosInvalidosException.class);
            assertThatThrownBy(() -> claseService.crear(principal(admin), req(MANANA, "06:00", "07:00", 20, cliente.getId())))
                    .isInstanceOf(DatosInvalidosException.class);
        }

        @Test
        void fechaPasadaEsInvalida() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);

            assertThatThrownBy(() -> claseService.crear(principal(entrenador),
                    req(LocalDate.now().minusDays(1), "06:00", "07:00", 20, null)))
                    .isInstanceOf(DatosInvalidosException.class);
        }

        @Test
        void horaFinDebeSerPosteriorAHoraInicio() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);

            assertThatThrownBy(() -> claseService.crear(principal(entrenador), req(MANANA, "07:00", "07:00", 20, null)))
                    .isInstanceOf(DatosInvalidosException.class);
        }

        @Test
        void entrenadorNoPuedeTenerDosClasesEnElMismoHorario() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
            claseService.crear(principal(entrenador), req(MANANA, "06:00", "07:00", 20, null));

            assertThatThrownBy(() -> claseService.crear(principal(entrenador), req(MANANA, "06:30", "07:30", 20, null)))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void editarUnaClaseNoChocaConsigoMisma() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
            ClaseResponse c = claseService.crear(principal(entrenador), req(MANANA, "06:00", "07:00", 20, null));

            ClaseResponse editada = claseService.actualizar(principal(entrenador), c.id(), req(MANANA, "06:00", "07:30", 25, null));

            assertThat(editada.aforoMaximo()).isEqualTo(25);
            assertThat(editada.horaFin()).isEqualTo(LocalTime.of(7, 30));
        }

        @Test
        void noSePuedeReducirElAforoPorDebajoDeLasReservas() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
            Clase clase = datos.clase(entrenador, MANANA, "10:00", "11:00", 5);
            reservaService.reservar(datos.clienteConMembresia().getId(), clase.getId());
            reservaService.reservar(datos.clienteConMembresia().getId(), clase.getId());

            assertThatThrownBy(() -> claseService.actualizar(principal(entrenador), clase.getId(),
                    req(MANANA, "10:00", "11:00", 1, null)))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("aforo");
        }

        @Test
        void entrenadorNoEditaClasesAjenas() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Usuario otro = datos.usuario(Rol.ENTRENADOR);

            assertThatThrownBy(() -> claseService.actualizar(principal(otro), clase.getId(),
                    req(MANANA, "10:00", "11:00", 5, null)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void noSeEliminaUnaClaseConReservasConfirmadas() {
            Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
            Clase clase = datos.clase(entrenador, MANANA, "10:00", "11:00", 5);
            reservaService.reservar(datos.clienteConMembresia().getId(), clase.getId());

            assertThatThrownBy(() -> claseService.eliminar(principal(entrenador), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void filtrosPorDisciplinaYHorario() {
            List<ClaseResponse> manana = claseService.listar(new ClaseFiltro(null, null, Horario.MANANA, null));
            List<ClaseResponse> yoga = claseService.listar(new ClaseFiltro(Disciplina.YOGA, null, null, null));

            assertThat(manana).isNotEmpty().allMatch(c -> c.horaInicio().isBefore(LocalTime.NOON));
            assertThat(yoga).isNotEmpty().allMatch(c -> c.disciplina() == Disciplina.YOGA);
        }

        @Test
        void inscritosDeClaseAjenaNoVisiblesParaEntrenador() {
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);
            Usuario otro = datos.usuario(Rol.ENTRENADOR);
            Usuario recepcion = datos.usuario(Rol.RECEPCIONISTA);
            reservaService.reservar(datos.clienteConMembresia().getId(), clase.getId());

            assertThatThrownBy(() -> claseService.inscritos(principal(otro), clase.getId()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(claseService.inscritos(principal(recepcion), clase.getId())).hasSize(1);
        }
    }

    // =====================================================================================
    @Nested
    class SociosYMembresias {

        private SocioRequest socio(String dni, String correo) {
            return new SocioRequest("Nuevo", "Socio", dni, correo, "987654321", "Clave12345", null);
        }

        @Test
        void creaSocioConContraseniaEncriptada() {
            SocioResponse s = membresiaService.crearSocio(socio("45678912", "nuevo@correo.test"));

            assertThat(s.id()).isNotNull();
            assertThat(usuarioRepository.findById(s.id()).orElseThrow().getPassword()).startsWith("$2");
        }

        @Test
        void dniDuplicadoDevuelveConflicto() {
            membresiaService.crearSocio(socio("45678913", "a1@correo.test"));

            assertThatThrownBy(() -> membresiaService.crearSocio(socio("45678913", "a2@correo.test")))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("DNI");
        }

        @Test
        void correoDuplicadoDevuelveConflictoSinImportarMayusculas() {
            membresiaService.crearSocio(socio("45678914", "b1@correo.test"));

            assertThatThrownBy(() -> membresiaService.crearSocio(socio("45678915", "B1@Correo.test")))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("correo");
        }

        @Test
        void editarSocioConservandoSuPropioDniNoEsConflicto() {
            SocioResponse s = membresiaService.crearSocio(socio("45678916", "c1@correo.test"));

            SocioResponse editado = membresiaService.actualizarSocio(s.id(),
                    new SocioRequest("Editado", "Socio", "45678916", "c1@correo.test", "912345678", null, false));

            assertThat(editado.nombres()).isEqualTo("Editado");
            assertThat(editado.activo()).isFalse();
        }

        @Test
        void rn05_vencimientoEsInicioMasDuracionDelPlan() {
            Usuario cliente = datos.usuario(Rol.CLIENTE);
            Plan plan = datos.planMensual();
            LocalDate inicio = LocalDate.now().plusDays(1);

            MembresiaResponse m = membresiaService.asignarMembresia(cliente.getId(), new MembresiaRequest(plan.getId(), inicio));

            assertThat(m.fechaVencimiento()).isEqualTo(inicio.plusDays(plan.getDuracionDias()));
            assertThat(m.estado()).isEqualTo(EstadoMembresia.ACTIVA);
        }

        @Test
        void noSeAsignanDosMembresiasActivasQueSeCrucen() {
            Usuario cliente = datos.clienteConMembresia();

            assertThatThrownBy(() -> membresiaService.asignarMembresia(cliente.getId(),
                    new MembresiaRequest(datos.planMensual().getId(), LocalDate.now().plusDays(10))))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void noSeActivaUnaMembresiaYaVencida() {
            Membresia m = datos.membresia(datos.usuario(Rol.CLIENTE), LocalDate.now().minusDays(60), EstadoMembresia.VENCIDA);

            assertThatThrownBy(() -> membresiaService.cambiarEstado(m.getId(), EstadoMembresia.ACTIVA))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void congelarMembresiaImpideReservar() {
            Usuario cliente = datos.usuario(Rol.CLIENTE);
            Membresia m = datos.membresia(cliente, LocalDate.now(), EstadoMembresia.ACTIVA);
            Clase clase = datos.clase(datos.usuario(Rol.ENTRENADOR), MANANA, "10:00", "11:00", 5);

            membresiaService.cambiarEstado(m.getId(), EstadoMembresia.CONGELADA);

            assertThatThrownBy(() -> reservaService.reservar(cliente.getId(), clase.getId()))
                    .isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        void rn06_marcaVencidasYAvisaLasQueVencenPronto() {
            Usuario vencido = datos.usuario(Rol.CLIENTE);
            Membresia expirada = datos.membresia(vencido, LocalDate.now().minusDays(35), EstadoMembresia.ACTIVA);
            Usuario porVencer = datos.usuario(Rol.CLIENTE);
            datos.membresia(porVencer, LocalDate.now().minusDays(28), EstadoMembresia.ACTIVA); // vence en 2 días

            int[] resultado = membresiaService.procesarVencimientos();

            assertThat(membresiaRepository.findById(expirada.getId()).orElseThrow().getEstado())
                    .isEqualTo(EstadoMembresia.VENCIDA);
            assertThat(resultado[0]).isGreaterThanOrEqualTo(1);
            assertThat(resultado[1]).isGreaterThanOrEqualTo(1);
            verify(notificaciones, atLeastOnce()).avisarVencimiento(eq(porVencer.getCorreo()), anyString(),
                    anyString(), eq(LocalDate.now().plusDays(2)), eq(2L));
        }

        @Test
        void miMembresiaDevuelveLaVigente() {
            Usuario cliente = datos.clienteConMembresia();

            MembresiaResponse m = membresiaService.miMembresia(cliente.getId());

            assertThat(m.estado()).isEqualTo(EstadoMembresia.ACTIVA);
            assertThat(m.diasRestantes()).isEqualTo(30);
        }
    }
}
