package com.sammyfitness.gym.config;

import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga datos FICTICIOS de demostración (solo perfil "dev" y solo si la BD está vacía):
 * 1 administrador, 1 recepcionista, 4 entrenadores, 10 clientes, 2 planes, 8 clases.
 * La contraseña de todos los usuarios de prueba se toma de la variable SEED_PASSWORD.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PlanRepository planRepository;
    private final MembresiaRepository membresiaRepository;
    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password:}")
    private String seedPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("DataSeeder: la base de datos ya tiene datos, no se cargan datos de prueba.");
            return;
        }
        if (seedPassword == null || seedPassword.isBlank()) {
            log.warn("DataSeeder: SEED_PASSWORD no está definida; no se cargan datos de prueba.");
            return;
        }

        String hash = passwordEncoder.encode(seedPassword);
        LocalDate hoy = LocalDate.now();

        // ---------- Personal ----------
        usuario("Sandra", "Medina Paredes", "70000001", "admin@sammyfitness.test", "900000001", Rol.ADMINISTRADOR, hash);
        usuario("Rosa", "Quispe Huamán", "70000002", "recepcion@sammyfitness.test", "900000002", Rol.RECEPCIONISTA, hash);

        Usuario ent1 = usuario("Carlos", "Ramos Díaz", "70000011", "carlos.ramos@sammyfitness.test", "900000011", Rol.ENTRENADOR, hash);
        Usuario ent2 = usuario("Lucía", "Torres Vega", "70000012", "lucia.torres@sammyfitness.test", "900000012", Rol.ENTRENADOR, hash);
        Usuario ent3 = usuario("Miguel", "Vargas Rojas", "70000013", "miguel.vargas@sammyfitness.test", "900000013", Rol.ENTRENADOR, hash);
        Usuario ent4 = usuario("Andrea", "Flores León", "70000014", "andrea.flores@sammyfitness.test", "900000014", Rol.ENTRENADOR, hash);

        // ---------- Clientes ----------
        String[][] datosClientes = {
                {"Juan", "Pérez Gómez"}, {"María", "López Castro"}, {"Pedro", "Sánchez Ruiz"},
                {"Ana", "Ramírez Soto"}, {"Luis", "Chávez Mendoza"}, {"Carmen", "Silva Ortiz"},
                {"Jorge", "Castillo Núñez"}, {"Elena", "Morales Ríos"}, {"Diego", "Herrera Campos"},
                {"Valeria", "Gutiérrez Paz"}
        };
        List<Usuario> clientes = new ArrayList<>();
        for (int i = 0; i < datosClientes.length; i++) {
            int n = i + 1;
            clientes.add(usuario(datosClientes[i][0], datosClientes[i][1],
                    String.format("7100%04d", n),
                    String.format("cliente%02d@correo.test", n),
                    String.format("91000%04d", n),
                    Rol.CLIENTE, hash));
        }

        // ---------- Planes ----------
        Plan mensual = planRepository.save(Plan.builder().nombre("Mensual").duracionDias(30).precio(new BigDecimal("120.00")).build());
        Plan trimestral = planRepository.save(Plan.builder().nombre("Trimestral").duracionDias(90).precio(new BigDecimal("320.00")).build());

        // ---------- Membresías ----------
        // Clientes 1-7: mensual activa; cliente 8: trimestral activa;
        // cliente 9: activa que vence en 2 días (aviso RN-06); cliente 10: vencida (caso PE-08).
        for (int i = 0; i < 7; i++) {
            membresia(clientes.get(i), mensual, hoy.minusDays(5), EstadoMembresia.ACTIVA);
        }
        membresia(clientes.get(7), trimestral, hoy.minusDays(10), EstadoMembresia.ACTIVA);
        membresia(clientes.get(8), mensual, hoy.minusDays(28), EstadoMembresia.ACTIVA);
        membresia(clientes.get(9), mensual, hoy.minusDays(40), EstadoMembresia.VENCIDA);

        // ---------- Clases ----------
        Clase c1 = clase("Spinning Power", Disciplina.SPINNING, ent1, hoy.plusDays(1), "07:00", "08:00", 20, "Ciclismo indoor de alta intensidad.");
        Clase c2 = clase("Crossfit WOD", Disciplina.CROSSFIT, ent2, hoy.plusDays(1), "18:00", "19:00", 15, "Entrenamiento funcional del día.");
        clase("Yoga Flow", Disciplina.YOGA, ent3, hoy.plusDays(1), "19:00", "20:00", 12, "Secuencias fluidas de respiración y movimiento.");
        clase("Funcional Total", Disciplina.FUNCIONAL, ent4, hoy.plusDays(2), "07:00", "08:00", 20, "Circuito de fuerza y resistencia.");
        clase("Zumba Fiesta", Disciplina.ZUMBA, ent3, hoy.plusDays(2), "19:00", "20:00", 25, "Baile aeróbico con ritmos latinos.");
        Clase c6 = clase("Halterofilia Técnica", Disciplina.HALTEROFILIA, ent2, hoy.plusDays(3), "08:00", "09:30", 2, "Técnica de arranque y envión. Cupos reducidos.");
        clase("Spinning Interval", Disciplina.SPINNING, ent1, hoy.plusDays(3), "18:00", "19:00", 20, "Intervalos de velocidad y resistencia.");
        clase("Yoga Restaurativo", Disciplina.YOGA, ent4, hoy.plusDays(4), "09:00", "10:00", 10, "Posturas suaves para recuperación.");

        // ---------- Reservas ----------
        reserva(clientes.get(0), c1);
        reserva(clientes.get(1), c1);
        reserva(clientes.get(2), c1);
        reserva(clientes.get(3), c2);
        // Clase c6 queda llena (aforo 2) para probar PE-07
        reserva(clientes.get(0), c6);
        reserva(clientes.get(1), c6);

        log.info("DataSeeder: datos de prueba cargados ({} usuarios, {} planes, {} clases, {} reservas).",
                usuarioRepository.count(), planRepository.count(), claseRepository.count(), reservaRepository.count());
    }

    private Usuario usuario(String nombres, String apellidos, String dni, String correo,
                            String telefono, Rol rol, String hash) {
        return usuarioRepository.save(Usuario.builder()
                .nombres(nombres).apellidos(apellidos).dni(dni).correo(correo)
                .telefono(telefono).password(hash).rol(rol).activo(true)
                .build());
    }

    private void membresia(Usuario usuario, Plan plan, LocalDate inicio, EstadoMembresia estado) {
        membresiaRepository.save(Membresia.builder()
                .usuario(usuario).plan(plan).fechaInicio(inicio).estado(estado)
                .build());
    }

    private Clase clase(String nombre, Disciplina disciplina, Usuario entrenador, LocalDate fecha,
                        String inicio, String fin, int aforo, String descripcion) {
        return claseRepository.save(Clase.builder()
                .nombre(nombre).disciplina(disciplina).entrenador(entrenador).fecha(fecha)
                .horaInicio(LocalTime.parse(inicio)).horaFin(LocalTime.parse(fin))
                .aforoMaximo(aforo).descripcion(descripcion)
                .build());
    }

    private void reserva(Usuario cliente, Clase clase) {
        reservaRepository.save(Reserva.builder()
                .cliente(cliente).clase(clase).estado(EstadoReserva.CONFIRMADA)
                .build());
    }
}
