package com.sammyfitness.gym.config;

import com.sammyfitness.gym.model.EstadoReserva;
import com.sammyfitness.gym.model.Rol;
import com.sammyfitness.gym.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Arranca el contexto completo con H2 y perfil dev para verificar los datos ficticios. */
@SpringBootTest
@ActiveProfiles({"test", "dev"})
class DataSeederTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private ClaseRepository claseRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void cargaLosDatosFicticiosEsperados() {
        assertThat(usuarioRepository.countByRol(Rol.ADMINISTRADOR)).isEqualTo(1);
        assertThat(usuarioRepository.countByRol(Rol.RECEPCIONISTA)).isEqualTo(1);
        assertThat(usuarioRepository.countByRol(Rol.ENTRENADOR)).isEqualTo(4);
        assertThat(usuarioRepository.countByRol(Rol.CLIENTE)).isEqualTo(10);
        assertThat(planRepository.count()).isEqualTo(2);
        assertThat(membresiaRepository.count()).isEqualTo(10);
        assertThat(claseRepository.count()).isEqualTo(8);
        assertThat(reservaRepository.count()).isGreaterThan(0);
    }

    @Test
    void lasContraseniasSeGuardanConBCrypt() {
        var admin = usuarioRepository.findByCorreoIgnoreCase("admin@sammyfitness.test").orElseThrow();

        assertThat(admin.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("Test12345", admin.getPassword())).isTrue();
    }

    @Test
    void existeUnaClaseLlenaParaProbarAforo() {
        boolean hayClaseLlena = claseRepository.findAll().stream().anyMatch(c ->
                reservaRepository.countByClaseIdAndEstado(c.getId(), EstadoReserva.CONFIRMADA) >= c.getAforoMaximo());

        assertThat(hayClaseLlena).isTrue();
    }
}
