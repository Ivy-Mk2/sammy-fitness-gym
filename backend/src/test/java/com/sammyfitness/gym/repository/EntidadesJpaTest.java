package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class EntidadesJpaTest {

    @Autowired private TestEntityManager em;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClaseRepository claseRepository;
    @Autowired private ReservaRepository reservaRepository;

    private Usuario nuevoUsuario(String dni, String correo, Rol rol) {
        return Usuario.builder()
                .nombres("Test").apellidos("Usuario").dni(dni).correo(correo)
                .telefono("999888777").password("$2a$10$hash").rol(rol)
                .build();
    }

    @Test
    void guardaUsuarioYLoBuscaPorCorreo() {
        usuarioRepository.save(nuevoUsuario("12345678", "test@correo.test", Rol.CLIENTE));

        assertThat(usuarioRepository.findByCorreoIgnoreCase("TEST@correo.test")).isPresent();
        assertThat(usuarioRepository.existsByDni("12345678")).isTrue();
        assertThat(usuarioRepository.findByCorreoIgnoreCase("TEST@correo.test").get().isActivo()).isTrue();
    }

    @Test
    void dniDuplicadoViolaRestriccionUnica() {
        usuarioRepository.saveAndFlush(nuevoUsuario("11111111", "a@correo.test", Rol.CLIENTE));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(nuevoUsuario("11111111", "b@correo.test", Rol.CLIENTE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void correoDuplicadoViolaRestriccionUnica() {
        usuarioRepository.saveAndFlush(nuevoUsuario("22222222", "dup@correo.test", Rol.CLIENTE));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(nuevoUsuario("33333333", "dup@correo.test", Rol.CLIENTE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void membresiaCalculaFechaVencimientoSegunPlan() {
        Usuario cliente = usuarioRepository.save(nuevoUsuario("44444444", "m@correo.test", Rol.CLIENTE));
        Plan plan = planRepository.save(Plan.builder().nombre("Mensual").duracionDias(30).precio(new BigDecimal("120.00")).build());
        LocalDate inicio = LocalDate.of(2026, 1, 15);

        Membresia m = membresiaRepository.saveAndFlush(Membresia.builder()
                .usuario(cliente).plan(plan).fechaInicio(inicio).build());

        assertThat(m.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 2, 14));
        assertThat(m.getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(m.estaVigenteEn(LocalDate.of(2026, 2, 1))).isTrue();
        assertThat(m.estaVigenteEn(LocalDate.of(2026, 2, 15))).isFalse();
    }

    @Test
    void claseIncrementaVersionAlActualizar() {
        Usuario entrenador = usuarioRepository.save(nuevoUsuario("55555555", "e@correo.test", Rol.ENTRENADOR));
        Clase clase = claseRepository.saveAndFlush(Clase.builder()
                .nombre("Yoga Test").disciplina(Disciplina.YOGA).entrenador(entrenador)
                .fecha(LocalDate.now().plusDays(1))
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0))
                .aforoMaximo(10).build());
        Long versionInicial = clase.getVersion();

        clase.setAforoMaximo(12);
        claseRepository.saveAndFlush(clase);

        assertThat(clase.getVersion()).isEqualTo(versionInicial + 1);
    }

    @Test
    void reservaSeGuardaConfirmadaYSeCuentaPorClase() {
        Usuario entrenador = usuarioRepository.save(nuevoUsuario("66666666", "e2@correo.test", Rol.ENTRENADOR));
        Usuario cliente = usuarioRepository.save(nuevoUsuario("77777777", "c@correo.test", Rol.CLIENTE));
        Clase clase = claseRepository.save(Clase.builder()
                .nombre("Spinning Test").disciplina(Disciplina.SPINNING).entrenador(entrenador)
                .fecha(LocalDate.now().plusDays(1))
                .horaInicio(LocalTime.of(7, 0)).horaFin(LocalTime.of(8, 0))
                .aforoMaximo(5).build());

        Reserva r = reservaRepository.saveAndFlush(Reserva.builder().cliente(cliente).clase(clase).build());
        em.clear();

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(r.getFechaReserva()).isNotNull();
        assertThat(reservaRepository.countByClaseIdAndEstado(clase.getId(), EstadoReserva.CONFIRMADA)).isEqualTo(1);
        assertThat(reservaRepository.findByClaseIdAndEstadoIn(clase.getId(), List.of(EstadoReserva.CANCELADA))).isEmpty();
    }
}
