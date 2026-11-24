package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.ReservaResponse;
import com.sammyfitness.gym.exception.ConflictoNegocioException;
import com.sammyfitness.gym.exception.RecursoNoEncontradoException;
import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.ClaseRepository;
import com.sammyfitness.gym.repository.MembresiaRepository;
import com.sammyfitness.gym.repository.ReservaRepository;
import com.sammyfitness.gym.repository.UsuarioRepository;
import com.sammyfitness.gym.service.correo.ReservaConfirmadaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de ReservaService (RN-01 a RN-04) con repositorios simulados (Mockito)
 * y un reloj fijo: "ahora" es el 01/12/2026 a las 10:00 en Lima.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate HOY = LocalDate.of(2026, 12, 1);
    private static final Clock RELOJ = Clock.fixed(LocalDateTime.of(HOY, LocalTime.of(10, 0)).atZone(LIMA).toInstant(), LIMA);

    @Mock ReservaRepository reservaRepository;
    @Mock ClaseRepository claseRepository;
    @Mock MembresiaRepository membresiaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ApplicationEventPublisher eventos;

    private ReservaService service;
    private Usuario cliente;
    private Usuario entrenador;
    private Clase clase;

    @BeforeEach
    void preparar() {
        service = new ReservaService(reservaRepository, claseRepository, membresiaRepository, usuarioRepository,
                eventos, new DtoMapper(RELOJ), RELOJ);
        cliente = Usuario.builder().id(1L).nombres("Ana").apellidos("Soto").correo("ana@correo.test").rol(Rol.CLIENTE).build();
        entrenador = Usuario.builder().id(2L).nombres("Carlos").apellidos("Ramos").rol(Rol.ENTRENADOR).build();
        clase = claseEn(HOY.plusDays(1), "10:00", "11:00", 2);
    }

    private Clase claseEn(LocalDate fecha, String inicio, String fin, int aforo) {
        return Clase.builder().id(10L).nombre("Yoga Flow").disciplina(Disciplina.YOGA).entrenador(entrenador)
                .fecha(fecha).horaInicio(LocalTime.parse(inicio)).horaFin(LocalTime.parse(fin))
                .aforoMaximo(aforo).version(0L).build();
    }

    private void existenClienteYClase() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(claseRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(clase));
    }

    private void membresiaActiva() {
        when(membresiaRepository.tieneMembresiaActivaEn(1L, clase.getFecha())).thenReturn(true);
    }

    // ============================================================ Reservar
    @Nested
    @DisplayName("Reservar")
    class Reservar {

        @Test
        @DisplayName("Reserva válida: se guarda CONFIRMADA y se publica el evento del correo")
        void reservaValida() {
            existenClienteYClase();
            membresiaActiva();
            when(reservaRepository.countByClaseIdAndEstadoIn(eq(10L), anyCollection())).thenReturn(1L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                r.setId(100L);
                return r;
            });

            ReservaResponse r = service.reservar(1L, 10L);

            assertThat(r.id()).isEqualTo(100L);
            assertThat(r.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(r.cancelable()).isTrue();
            ArgumentCaptor<ReservaConfirmadaEvent> evento = ArgumentCaptor.forClass(ReservaConfirmadaEvent.class);
            verify(eventos).publishEvent(evento.capture());
            assertThat(evento.getValue().correo()).isEqualTo("ana@correo.test");
            assertThat(evento.getValue().clase()).isEqualTo("Yoga Flow");
        }

        @Test
        @DisplayName("RN-01: sin membresía ACTIVA en la fecha de la clase → 409 y no se guarda")
        void rn01SinMembresiaActiva() {
            existenClienteYClase();
            // tieneMembresiaActivaEn devuelve false por defecto

            assertThatThrownBy(() -> service.reservar(1L, 10L))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("membresía activa");
            verify(reservaRepository, never()).save(any());
            verifyNoInteractions(eventos);
        }

        @Test
        @DisplayName("RN-02: aforo completo → 409")
        void rn02AforoCompleto() {
            existenClienteYClase();
            membresiaActiva();
            when(reservaRepository.countByClaseIdAndEstadoIn(eq(10L), anyCollection())).thenReturn(2L);

            assertThatThrownBy(() -> service.reservar(1L, 10L))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("aforo completo");
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("RN-02: la clase se lee con bloqueo optimista (incrementa @Version)")
        void rn02UsaBloqueoOptimista() {
            existenClienteYClase();

            assertThatThrownBy(() -> service.reservar(1L, 10L)).isInstanceOf(ConflictoNegocioException.class);

            verify(claseRepository).findByIdConBloqueo(10L);
            verify(claseRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("RN-03: reservar dos veces la misma clase → 409")
        void rn03ReservaDuplicada() {
            existenClienteYClase();
            membresiaActiva();
            when(reservaRepository.existsByClienteIdAndClaseIdAndEstadoIn(eq(1L), eq(10L), anyCollection())).thenReturn(true);

            assertThatThrownBy(() -> service.reservar(1L, 10L))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("Ya tiene una reserva");
        }

        @Test
        @DisplayName("RN-03: otra clase reservada en el mismo horario → 409")
        void rn03CruceDeHorario() {
            existenClienteYClase();
            membresiaActiva();
            when(reservaRepository.existeCruceHorarioCliente(eq(1L), anyCollection(), eq(clase.getFecha()),
                    eq(clase.getHoraInicio()), eq(clase.getHoraFin()))).thenReturn(true);

            assertThatThrownBy(() -> service.reservar(1L, 10L))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("mismo horario");
        }

        @Test
        @DisplayName("No se reserva una clase que ya empezó → 409")
        void claseYaIniciada() {
            clase = claseEn(HOY, "09:30", "10:30", 20);
            existenClienteYClase();

            assertThatThrownBy(() -> service.reservar(1L, 10L))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("ya se realizó");
            verifyNoInteractions(membresiaRepository);
        }

        @Test
        @DisplayName("Clase inexistente → 404")
        void claseInexistente() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(claseRepository.findByIdConBloqueo(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reservar(1L, 99L)).isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    // ============================================================ Cancelar
    @Nested
    @DisplayName("Cancelar (RN-04)")
    class Cancelar {

        private Reserva reserva(Usuario duenio, EstadoReserva estado) {
            Reserva r = Reserva.builder().id(100L).cliente(duenio).clase(clase).estado(estado)
                    .fechaReserva(LocalDateTime.now(RELOJ)).build();
            when(reservaRepository.findById(100L)).thenReturn(Optional.of(r));
            return r;
        }

        @Test
        @DisplayName("Cancelar la reserva propia la deja CANCELADA (libera el cupo)")
        void cancelarPropia() {
            Reserva r = reserva(cliente, EstadoReserva.CONFIRMADA);

            ReservaResponse resp = service.cancelar(1L, 100L);

            assertThat(resp.estado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(resp.cancelable()).isFalse();
        }

        @Test
        @DisplayName("Cancelar una reserva de otro cliente → 403")
        void cancelarAjena() {
            Usuario otro = Usuario.builder().id(5L).rol(Rol.CLIENTE).build();
            Reserva r = reserva(otro, EstadoReserva.CONFIRMADA);

            assertThatThrownBy(() -> service.cancelar(1L, 100L)).isInstanceOf(AccessDeniedException.class);
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        @DisplayName("Cancelar una reserva ya cancelada → 409")
        void cancelarYaCancelada() {
            reserva(cliente, EstadoReserva.CANCELADA);

            assertThatThrownBy(() -> service.cancelar(1L, 100L)).isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        @DisplayName("No se cancela una clase ya realizada → 409")
        void cancelarClaseRealizada() {
            clase = claseEn(HOY.minusDays(1), "18:00", "19:00", 20);
            Reserva r = reserva(cliente, EstadoReserva.CONFIRMADA);

            assertThatThrownBy(() -> service.cancelar(1L, 100L))
                    .isInstanceOf(ConflictoNegocioException.class)
                    .hasMessageContaining("ya se realizó");
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        @DisplayName("No se cancela una clase en curso → 409")
        void cancelarClaseEnCurso() {
            clase = claseEn(HOY, "09:30", "10:30", 20);
            reserva(cliente, EstadoReserva.CONFIRMADA);

            assertThatThrownBy(() -> service.cancelar(1L, 100L)).isInstanceOf(ConflictoNegocioException.class);
        }

        @Test
        @DisplayName("Reserva inexistente → 404")
        void reservaInexistente() {
            when(reservaRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelar(1L, 404L)).isInstanceOf(RecursoNoEncontradoException.class);
        }
    }
}
